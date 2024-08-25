package com.taskmanager.controller;

import com.taskmanager.domain.Project;
import com.taskmanager.domain.Task;
import com.taskmanager.domain.User;
import com.taskmanager.dto.ProjectDTO;
import com.taskmanager.dto.TaskDTO;
import com.taskmanager.dto.UserDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class Utils {
    public static <T> void updateFieldIfNotNull(Consumer<T> setter, T value) {
        if (value != null) {
            setter.accept(value);
        }
    }

    public static List<TaskDTO> getTaskDTOs(List<Task> tasks) {
        List<TaskDTO> taskDTOS = new ArrayList<>();
        tasks.forEach(task -> taskDTOS.add(getTaskDTO(task)));
        return taskDTOS;
    }

    public static List<ProjectDTO> getProjectDTOs(List<Project> projects) {
        List<ProjectDTO> projectDTOS = new ArrayList<>();
        projects.forEach(project -> projectDTOS.add(getProjectDTO(project)));
        return projectDTOS;
    }

    public static UserDTO getUserDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail()
        );
    }

    public static ProjectDTO getProjectDTO(Project project) {
        return new ProjectDTO(
                project.getId(),
                project.getTitle(),
                project.getDescription(),
                project.getOwner() == null ? null : project.getOwner().getEmail(),
                getTaskDTOs(project.getTasks().stream().toList())
        );
    }

    public static TaskDTO getTaskDTO(Task task) {
        return new TaskDTO(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getCreator() == null ? null : task.getCreator().getId(),
                task.getAssignee() == null ? null : task.getAssignee().getId()
        );
    }

    public static <T, D> ResponseEntity<?> getEntityResponse(
            Long id,
            Function<Long, T> serviceFunction,
            Function<T, D> dtoFunction,
            String entityName
    ) {
        T entity = serviceFunction.apply(id);
        if (entity == null) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", entityName + " not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
        return ResponseEntity.ok(dtoFunction.apply(entity));
    }

    public static <T> ResponseEntity<?> deleteEntity(
            Long id,
            Function<Long, T> serviceFunction,
            Consumer<Long> deleteFunction,
            Function<T, String> ownerEmailFunction,
            String entityName
    ) {
        T entity = serviceFunction.apply(id);
        Map<String, String> errorResponse = new HashMap<>();
        if (entity == null) {
            errorResponse.put("error", entityName + " not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = ((UserDetails) authentication.getPrincipal()).getUsername();
        String ownerEmail = ownerEmailFunction.apply(entity);

        if (!Objects.equals(ownerEmail, currentUsername)) {
            errorResponse.put("error", "You are not owner of this " + entityName.toLowerCase() + ".");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        }

        deleteFunction.accept(id);
        return ResponseEntity.ok(entityName + " has been deleted successfully.");
    }

    public static ResponseEntity<?> createErrorResponse(String message, HttpStatus status) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", message);
        return ResponseEntity.status(status).body(errorResponse);
    }

    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return ((UserDetails) authentication.getPrincipal()).getUsername();
    }
}
