package com.forvmom.core.services;

import com.forvmom.common.errorhandler.ResourceNotFoundException;
import com.forvmom.data.dao.ReOrderingDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maintains the contiguous 1-based display ordering used by admin-sortable
 * catalog entities (categories, sub-categories, media, and similar).
 *
 * <p>
 * Rather than storing fractional or sparse positions, moving an item shifts the
 * block of rows it passes over, so positions stay dense and gap-free. Both the
 * shift and the move happen in a single transaction — a partial shift would
 * leave duplicate or missing positions.
 *
 * <p>
 * The entity type is passed as a {@link Class} because the DAO resolves the
 * target table dynamically, letting one implementation serve every orderable
 * entity.
 */
@Service
@Transactional
public class ReorderingService {

    @Autowired
    private ReOrderingDao reOrderingDao;

    /**
     * Moves an entity to a new 1-based position, shifting the rows in between.
     *
     * <p>
     * Direction matters: moving down shifts the intervening rows up, moving up
     * shifts them down. Doing it in one direction only would corrupt the sequence.
     *
     * @param entityId    identifier of the entity to move
     * @param newPosition target position, between 1 and the current maximum
     * @param entityType  entity class, used to resolve the target table
     * @throws ResourceNotFoundException if the entity or its position is missing
     * @throws IllegalArgumentException  if {@code newPosition} is out of range
     */
    public void reorderItems(Long entityId, Long newPosition, Class entityType) {
        // First, get the current position of the entity
        Long currentPosition = reOrderingDao.getCurrentPosition(entityId, entityType);

        if (currentPosition == null) {
            throw new ResourceNotFoundException("Entity or position is missing with id: " + entityId);
        }

        if (currentPosition.equals(newPosition)) {
            return; // Already at the desired position
        }

        // Get max position for validation
        Long maxPosition = reOrderingDao.getMaxOrder(entityType);
        if (newPosition < 1 || newPosition > maxPosition) {
            throw new IllegalArgumentException(
                    "New position " + newPosition + " is invalid. Must be between 1 and " + maxPosition
            );
        }

        // Determine direction and move
        if (newPosition > currentPosition) {
            reOrderingDao.moveDown(entityId, currentPosition, newPosition, entityType);
        } else {
            reOrderingDao.moveUp(entityId,currentPosition, newPosition, entityType);
        }
    }

    /**
     * Returns the highest position currently in use, i.e. the number of orderable
     * rows for this entity type.
     *
     * @param entityType entity class, used to resolve the target table
     * @return the maximum position, or {@code null} if there are no rows
     */
    public Long getMaxOrder(Class entityType) {
        return reOrderingDao.getMaxOrder(entityType);
    }

    /**
     * Returns the current position of a single entity.
     *
     * @param entityId   identifier of the entity
     * @param entityType entity class, used to resolve the target table
     * @return the current 1-based position, or {@code null} if unknown
     */
    public Long getCurrentPosition(Long entityId, Class entityType) {
        return reOrderingDao.getCurrentPosition(entityId, entityType);
    }
}