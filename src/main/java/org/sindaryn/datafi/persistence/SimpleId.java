package org.sindaryn.datafi.persistence;

import lombok.NoArgsConstructor;

import javax.persistence.Embeddable;
import java.io.Serializable;

@lombok.Getter @lombok.Setter
@Embeddable
@NoArgsConstructor
public class SimpleId implements Serializable{
    private Long id = IdFactory.getNextId();
    @Override
    public String toString(){
        return this.id.toString();
    }
}
