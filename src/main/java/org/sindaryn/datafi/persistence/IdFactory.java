package org.sindaryn.datafi.persistence;

public class IdFactory {
    private static final SequenceGenerator sequenceGenerator = new SequenceGenerator();
    public static Long getNextId(){
        return sequenceGenerator.nextId();
    }
}
