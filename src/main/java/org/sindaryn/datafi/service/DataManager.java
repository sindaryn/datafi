package org.sindaryn.datafi.service;

import org.springframework.stereotype.Component;

/**
 * can be autowired into a service layer bean for
 * complete coverage of jpa repository operations
 * @param <T>
 */
@Component
public class DataManager<T> extends BaseDataManager<T> {
}
