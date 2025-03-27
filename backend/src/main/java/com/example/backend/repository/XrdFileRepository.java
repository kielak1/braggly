package com.example.backend.repository;

import com.example.backend.model.XrdFile;
import com.example.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface XrdFileRepository extends JpaRepository<XrdFile, Long> {
    List<XrdFile> findByUser(User user);
    List<XrdFile> findByPublicVisibleTrue();
}