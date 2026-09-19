package com.forvmom.core.event_enrichment;

import com.forvmom.common.dto.events.BookingRequestEvent;
import com.forvmom.common.helpers.BookingOutboxPayload;
import com.forvmom.common.helpers.BookingPricingSummary;
import com.forvmom.common.helpers.BookingSnapshotBundle;
import com.forvmom.core.retries.model.BookingOutboxProcessingClaim;
import com.forvmom.core.retries.service.BookingOutboxPublicationTransactionService;
import com.forvmom.core.retries.service.BookingOutboxStateService;
import com.forvmom.data.entities.BookingOutbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Builds and publishes the Kafka event stored in one booking outbox row.
 *
 * <p>Flow: claim with a new token -> enrich -> lock and check token -> publish
 * -> mark published. A failure becomes {@code FAILED} and Quartz retries it.
 * A worker with an old token cannot publish or change the row.
 */
@Component
public class BookingEnrichmentTask {

    private static final Logger logger =
            LoggerFactory.getLogger(BookingEnrichmentTask.class);

    /** Reads the small JSON payload stored in the outbox row. */
    private final BookingPayloadParser payloadParser;

    /** Loads the data needed to build the full event. */
    private final BookingSnapshotResolver snapshotResolver;

    /** Calculates booking and add-on prices. */
    private final BookingPricingCalculator pricingCalculator;

    /** Builds the outgoing Kafka event. */
    private final BookingRequestEventFactory eventFactory;

    private final BookingOutboxStateService bookingOutboxStateService;
    private final BookingOutboxPublicationTransactionService publicationTransactionService;

    public BookingEnrichmentTask(
            BookingPayloadParser payloadParser,
            BookingSnapshotResolver snapshotResolver,
            BookingPricingCalculator pricingCalculator,
            BookingRequestEventFactory eventFactory,
            BookingOutboxStateService bookingOutboxStateService,
            BookingOutboxPublicationTransactionService publicationTransactionService
    ) {
        this.payloadParser = payloadParser;
        this.snapshotResolver = snapshotResolver;
        this.pricingCalculator = pricingCalculator;
        this.eventFactory = eventFactory;
        this.bookingOutboxStateService = bookingOutboxStateService;
        this.publicationTransactionService = publicationTransactionService;
    }

    /**
     * Asynchronously enriches and publishes one outbox row.
     * The same method is used by the first attempt and Quartz retries.
     *
     * @param bookingReferenceId unique booking reference identifier
     */
    @Async("bookingTaskExecutor")
    public void enrich(String bookingReferenceId) {
        logger.info(
                "Starting enrichment for bookingReferenceId={}",
                bookingReferenceId);

        BookingOutboxProcessingClaim claim =
                bookingOutboxStateService.claimForProcessing(bookingReferenceId);
        if (claim == null) {
            logger.info(
                    "Could not claim outbox record because it is already processing or published: bookingReferenceId={}",
                    bookingReferenceId);
            return;
        }

        try {
            // Build the event only after this worker owns the row.
            BookingOutbox bookingOutbox =
                    bookingOutboxStateService.loadRequired(bookingReferenceId);
            BookingOutboxPayload payload =
                    payloadParser.parse(bookingOutbox);
            BookingSnapshotBundle snapshots =
                    snapshotResolver.resolve(payload);
            BookingPricingSummary pricing =
                    pricingCalculator.calculate(
                            payload,
                            snapshots);
            BookingRequestEvent event =
                    eventFactory.create(
                            bookingOutbox,
                            payload,
                            snapshots,
                            pricing);
            // The transaction checks the token again before sending to Kafka.
            boolean published = publicationTransactionService.publish(claim, event);
            if (!published) {
                logger.info(
                        "Outbox ownership expired before publication: bookingReferenceId={}",
                        bookingReferenceId);
                return;
            }
            logger.info(
                    "Enrichment complete and published: bookingReferenceId={}, eventId={}, correlationId={}",
                    bookingReferenceId,
                    bookingOutbox.getEventId(),
                    bookingOutbox.getCorrelationId());

        } catch (Exception exception) {
            logger.error(
                    "Enrichment failed for bookingReferenceId={}: {}",
                    bookingReferenceId,
                    exception.getMessage(),
                    exception);
            // This update succeeds only if this worker still owns the same token.
            boolean failed = bookingOutboxStateService.markFailed(
                    claim,
                    failureReason(exception));
            if (!failed) {
                logger.info(
                        "Outbox failure ignored because ownership changed: bookingReferenceId={}",
                        bookingReferenceId);
            }
        }
    }

    /**
     * Extracts the root failure while keeping persisted diagnostics bounded by
     * {@link BookingOutboxStateService}.
     */
    private String failureReason(Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return cause.getClass().getSimpleName() + (message == null ? "" : ": " + message);
    }
}