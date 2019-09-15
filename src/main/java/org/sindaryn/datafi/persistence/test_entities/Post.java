package org.sindaryn.datafi.persistence.test_entities;

import lombok.Data;
import org.sindaryn.datafi.persistence.IdFactory;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Data
@Entity
public class Post {
    @Id
    private Long id = IdFactory.getNextId();
    private String title;
    private String content;
    @ManyToOne
    private Users user;
}
