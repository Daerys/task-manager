package com.taskmanager.controller;

import com.taskmanager.domain.Project;
import com.taskmanager.domain.User;
import com.taskmanager.service.ProjectService;
import com.taskmanager.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static com.taskmanager.controller.Utils.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService projectService;
    private final UserService userService;

    @PostMapping("")
    public ResponseEntity<?> createProject(@Valid @RequestBody Project project, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(bindingResult.getAllErrors());
        }
        String currentUsername = getCurrentUsername();

        User user = userService.findByEmail(currentUsername);
        project.setOwner(user);

        return ResponseEntity.ok(getProjectDTO(projectService.saveProject(project)));
    }

    @GetMapping("")
    public ResponseEntity<?> getProjects() {
        return ResponseEntity.ok(getProjectDTOs(projectService.getProjects()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProject(@PathVariable Long id) {
        Project project = projectService.findById(id);
        if (project == null) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Project not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }

        return ResponseEntity.ok(getProjectDTO(project));
    }

    /**
     * Partially updates a project based on the provided project data. This method checks if the current user
     * is authorized to modify the project by comparing the owner's ID. The project will be updated only if the
     * authenticated user is the owner of the project.
     *
     * <p><strong>Important:</strong> The {@link User} object in the project should only have the {@code email} field
     * filled in, while all other fields should be left empty or null. This email is used to find the user
     * to be set as the owner of the project.</p>
     *
     * @param id      The ID of the project to be patched.
     * @param project The project data from the frontend, containing the updated fields.
     * @return A {@link ResponseEntity} containing the updated project if successful, or an error message
     * if the operation is forbidden or the project is not found.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<?> updateProject(@PathVariable Long id, @Valid @RequestBody Project project) {
        String currentUsername = getCurrentUsername();
        User currentUser = userService.findByEmail(currentUsername);

        Project projectDB = projectService.findById(id);
        if (projectDB == null) {
            return createErrorResponse("Project not found.", HttpStatus.NOT_FOUND);
        }

        if (!Objects.equals(currentUser.getEmail(), projectDB.getOwner().getEmail())) {
            return createErrorResponse("You have no permission to change this project.", HttpStatus.FORBIDDEN);
        }

        updateFieldIfNotNull(projectDB::setTitle, project.getTitle());
        updateFieldIfNotNull(projectDB::setDescription, project.getDescription());
        if (project.getOwner() != null && !Objects.equals(project.getOwner().getEmail(), projectDB.getOwner().getEmail())) {
            projectDB.setOwner(userService.findByEmail(project.getOwner().getEmail()));
        }

        return ResponseEntity.ok(getProjectDTO(projectService.saveProject(projectDB)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(@PathVariable Long id) {
        return deleteEntity(
                id,
                projectService::findById,
                projectService::deleteProject,
                project -> project.getOwner().getEmail(),
                "Project"
        );
    }
}
