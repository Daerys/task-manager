package com.taskmanager.controller;

import com.taskmanager.service.TaskService;
import com.taskmanager.service.UserService;
import com.taskmanager.domain.Task;
import com.taskmanager.domain.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;
    private final UserService userService;

    @PostMapping("")
    public ResponseEntity<?> createTask(@Valid @RequestBody Task task, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(bindingResult.getAllErrors());
        }

        String currentUsername = Utils.getCurrentUsername();
        User user = userService.findByEmail(currentUsername);
        userService.saveUser(user);

        task.setCreator(user);

        if (task.getAssignee() != null) {
            User assignee = userService.findByEmail(task.getAssignee().getEmail());
            if (assignee == null) {
                assignee = task.getAssignee();
                userService.saveUser(assignee);
            }
            task.setAssignee(assignee);
        }

        return ResponseEntity.ok().body(Utils.getTaskDTO(taskService.saveTask(task)));
    }

    @GetMapping("")
    public ResponseEntity<?> getTasks() {
        return ResponseEntity.ok(Utils.getTaskDTOs(taskService.getTasks()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTask(@PathVariable Long id) {
        return Utils.getEntityResponse(id, taskService::getTask, Utils::getTaskDTO, "Task");
    }

    /**
     * Partially updates a task based on the provided task data. This method checks if the current user
     * is authorized to modify the task by comparing the creator's ID. The task will be updated only if the
     * authenticated user is the creator of the task.
     *
     * <p><strong>Important:</strong> The {@link User} object in the task should only have the {@code email} field
     * filled in, while all other fields should be left empty or null. This email is used to find the user
     * to be assigned to the task.</p>
     *
     * @param id   The ID of the task to be patched.
     * @param task The task data from the frontend, containing the updated fields.
     * @return A {@link ResponseEntity} containing the updated task if successful, or an error message
     * if the operation is forbidden or the task is not found.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<?> patchTask(@PathVariable Long id, @RequestBody Task task) {
        String currentUsername = Utils.getCurrentUsername();
        User currentUser = userService.findByEmail(currentUsername);

        Task taskDB = taskService.getTask(id);

        if (taskDB == null) {
            return Utils.createErrorResponse("Task not found.", HttpStatus.NOT_FOUND);
        }

        if (!Objects.equals(currentUser.getEmail(), taskDB.getCreator().getEmail())) {
            return Utils.createErrorResponse("You have no permission to change this project.", HttpStatus.FORBIDDEN);
        }

        Utils.updateFieldIfNotNull(taskDB::setTitle, task.getTitle());
        Utils.updateFieldIfNotNull(taskDB::setDescription, task.getDescription());
        Utils.updateFieldIfNotNull(taskDB::setStatus, task.getStatus());
        Utils.updateFieldIfNotNull(taskDB::setPriority, task.getPriority());
        Utils.updateFieldIfNotNull(taskDB::setDueDate, task.getDueDate());

        if (task.getAssignee() != null && task.getAssignee().getEmail() != null) {
            taskDB.setAssignee(userService.findByEmail(task.getAssignee().getEmail()));
        }

        return ResponseEntity.ok(Utils.getTaskDTO(taskDB));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable Long id) {
        return Utils.deleteEntity(
                id,
                taskService::getTask,
                taskService::findById,
                task -> task.getCreator().getEmail(),
                "Task"
        );
    }
}
