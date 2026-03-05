package com.getapi.comment.service;

import org.springframework.stereotype.Service;

import com.getapi.comment.respository.PostCommentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostCommentService {
	private final PostCommentRepository postCommentRepository;
}
