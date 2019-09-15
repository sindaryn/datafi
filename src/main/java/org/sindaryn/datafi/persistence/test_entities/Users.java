package org.sindaryn.datafi.persistence.test_entities;

import lombok.Data;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.sindaryn.datafi.persistence.IdFactory;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.List;

@Entity
@Data
public class Users {
    @Id
    private Long id = IdFactory.getNextId();
    private String name;
    @OneToMany(mappedBy = "user")@Cascade(CascadeType.ALL)
    private List<Post> posts;
}
