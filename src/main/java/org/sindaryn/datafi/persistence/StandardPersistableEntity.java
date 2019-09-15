package org.sindaryn.datafi.persistence;

import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class StandardPersistableEntity extends BasePersistableEntity<SimpleId> {
    @Override
    public void initId() {
        this.id = new SimpleId();
    }
}
