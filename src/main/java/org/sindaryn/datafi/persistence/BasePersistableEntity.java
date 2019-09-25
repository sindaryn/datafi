package org.sindaryn.datafi.persistence;

import lombok.*;
import org.sindaryn.datafi.annotations.NonApiUpdatable;

import javax.persistence.*;
import java.io.Serializable;

/**
 * A convenient @MappedSuperclass which takes care of the boilerplate
 * code required to deal with the logic of persisting an entity
 * to a database - i.e. generating the id, marking a given
 * instance as non-archived, adding a 'createdAt' column.
 * @param <TID>
 */
@MappedSuperclass
@EqualsAndHashCode
@RequiredArgsConstructor
@NoArgsConstructor
@Getter
public abstract class BasePersistableEntity<TID> implements Serializable {
    /**
     * If directly inheriting from this class,
     * take note that only embedded id types
     * are compatible.
     */
    @EmbeddedId
    @NonNull
    @NonApiUpdatable
    @Column(name = "id", unique=true, nullable=false, updatable=false)
    protected TID id;
    @NonApiUpdatable
    private Boolean isFirstPersist = true;
    @Version
    @NonApiUpdatable
    private Long version = 0L;
    @PrePersist
    public void init(){
        if(isFirstPersist){
            initId();
            customFirstTimeInit();
            isFirstPersist = false;
        }
    }
    protected void customFirstTimeInit(){}
    public abstract void initId();
}
