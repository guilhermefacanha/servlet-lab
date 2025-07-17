package app.dao;

import app.annotations.ApplicationTenantDB;
import app.annotations.TransactionalResourceLocal;
import app.entity.User;
import lombok.extern.slf4j.Slf4j;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.io.Serializable;
import java.util.List;

@Slf4j
@Stateless
public class UserDao implements Serializable {

    private static final long serialVersionUID = 352749899047222475L;

    @ApplicationTenantDB
    @Inject
    private EntityManager entityManager;

    @TransactionalResourceLocal
    public User save(User user) {
        return entityManager.merge(user);
    }

    public User findById(Long id) {
        return entityManager.find(User.class, id);
    }

    public List<User> findAll() {
        return entityManager.createQuery("SELECT u FROM User u", User.class).getResultList();
    }

    @TransactionalResourceLocal
    public void delete(Long id) {
        User user = entityManager.find(User.class, id);
        if (user != null) {
            entityManager.remove(user);
        }
    }

    public User findByUsername(String username) {
        return entityManager.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                .setParameter("username", username)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }
}