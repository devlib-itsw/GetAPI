package com.DevLib.repository;

import com.DevLib.domain.User;
import java.util.List;

/**
 * 유저 저장소 인터페이스.
 * 현재: InMemoryUserStore (ArrayList)
 * 추후: JPA 구현체로 교체
 */
public interface UserStore {
    boolean existsByEmail(String email);
    void save(User user);
    List<User> findAll();
}
