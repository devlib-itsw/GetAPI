package com.DevLib.repository;

import com.DevLib.domain.User;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class InMemoryUserStore implements UserStore {
    private final List<User> users = new CopyOnWriteArrayList<>();

    @Override
    public boolean existsByEmail(String email) {
        return users.stream().anyMatch(u -> email.equals(u.getEmail()));
    }

    @Override
    public void save(User user) {
        users.add(user);
        System.out.println("--- 현재 임시 DB 목록 ---");
        for (User u : users) {
            System.out.println(u);
        }
    }

    @Override
    public List<User> findAll() {
        return Collections.unmodifiableList(users);
    }
}
