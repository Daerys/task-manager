package com.taskmanager.dto;


import com.taskmanager.domain.enums.Priority;
import com.taskmanager.domain.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskDTO {

    private Long id;

    private String title;

    private String description;

    private Status status = Status.TODO;

    private Priority priority = Priority.MEDIUM;

    private Long creatorId;

    private Long assigneeId;
}
