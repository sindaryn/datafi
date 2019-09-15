package org.sindaryn.datafi.service;

import lombok.Getter;
import org.sindaryn.datafi.persistence.GenericDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class DaoCollector {
    @Autowired
    @Getter
    private List<? extends GenericDao> daos;
}
