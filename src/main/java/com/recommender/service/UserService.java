package com.recommender.service;

import com.recommender.db.entity.UserEntity;
import com.recommender.db.repository.UserRepository;
import com.recommender.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepo;

    public UserService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Transactional
    public UserEntity save(User user) {
        UserEntity entity = new UserEntity(user.getName());
        return userRepo.save(entity);
    }

    /** Find existing user or create a new one by name. */
    @Transactional
    public UserEntity findOrCreate(String name) {
        return userRepo.findByName(name)
                .orElseGet(() -> userRepo.save(new UserEntity(name)));
    }

    public Optional<User> findById(UUID id) {
        return userRepo.findById(id).map(e -> {
            User u = new User(e.getId().toString(), e.getName());
            return u;
        });
    }

    public Optional<UserEntity> findEntityById(UUID id) {
        return userRepo.findById(id);
    }

    public List<UserEntity> findAll() {
        return userRepo.findAll();
    }
}
