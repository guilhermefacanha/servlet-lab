package controller;

import dao.UserDao;
import entity.User;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.annotation.View;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Named("userController")
@ViewScoped
public class UserController implements Serializable {

    @Inject
    private UserDao userDao;

    @Getter @Setter
    private List<User> users;

    @Getter @Setter
    private User selectedUser = new User();

    public void loadUsers() {
        users = userDao.findAll();
    }

    public void prepareNewUser() {
        selectedUser = new User();
    }

    public void editUser(User user) {
        selectedUser = user;
    }

    public void saveUser() {
        userDao.save(selectedUser);
        loadUsers();
    }

    public void deleteUser(Long id) {
        userDao.delete(id);
        loadUsers();
    }
}