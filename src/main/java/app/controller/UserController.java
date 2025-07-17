package app.controller;

import app.dao.UserDao;
import app.entity.User;
import lombok.Getter;
import lombok.Setter;

import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
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