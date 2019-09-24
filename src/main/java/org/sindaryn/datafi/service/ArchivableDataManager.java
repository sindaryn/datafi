package org.sindaryn.datafi.service;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.sindaryn.datafi.persistence.Archivable;

import java.util.Collection;
import java.util.List;

import static org.sindaryn.datafi.StaticUtils.*;


@SuppressWarnings("unchecked")
@NoArgsConstructor
public class ArchivableDataManager<T extends Archivable> extends BaseDataManager<T> {

    public ArchivableDataManager(@NonNull Class<T> clazz) {
        super(clazz);
    }

    public T archive(T input) {
        Object id = getId(input, reflectionCache);
        final String simpleName = input.getClass().getSimpleName();
        T toArchive = findById((Class<T>) input.getClass(), id).orElse(null);
        if(toArchive == null) throwEntityNotFoundException(simpleName, id);
        toArchive.setIsArchived(true);
        return save(toArchive);
    }
    public T deArchive(T input) {
        Object id = getId(input, reflectionCache);
        final String simpleName = input.getClass().getSimpleName();
        T toDeArchive = findById((Class<T>) input.getClass(), id).orElse(null);
        if(toDeArchive == null) throwEntityNotFoundException(simpleName, id);
        toDeArchive.setIsArchived(false);
        return save(toDeArchive);
    }
    public List<T> archiveCollection(Collection<T> input) {
        final Class<T> clazz = (Class<T>) input.iterator().next().getClass();
        List<Object> ids = getIdList(input, reflectionCache);
        List<T> toArchive = findAllById(clazz, ids);
        toArchive.forEach(item -> item.setIsArchived(true));
        return saveAll(toArchive);
    }
    public List<T> deArchiveCollection(Collection<T> input) {
        final Class<T> clazz = (Class<T>) input.iterator().next().getClass();
        List<Object> ids = getIdList(input, reflectionCache);
        List<T> toDeArchive = findAllById(clazz, ids);
        toDeArchive.forEach(item -> item.setIsArchived(false));
        return saveAll(toDeArchive);
    }
}
