package com.getapi.tag.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.getapi.tag.domain.Tag;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long>{

}
