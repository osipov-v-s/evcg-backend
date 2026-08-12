package com.profession.suggest.controllers.education;

import com.profession.suggest.configuration.security.annotation.HasRole;
import com.profession.suggest.database.entities.auth.role.RoleEnum;
import com.profession.suggest.database.services.education.CuratorService;
import com.profession.suggest.database.services.education.SchoolService;
import com.profession.suggest.dto.education.CuratorCreateRequestDTO;
import com.profession.suggest.dto.education.CuratorDTO;
import com.profession.suggest.dto.education.SchoolDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schools")
@RequiredArgsConstructor
public class SchoolController {
    private final SchoolService schoolService;
    private final CuratorService curatorService;

    @HasRole({RoleEnum.ADMIN, RoleEnum.PUPIL})
    @GetMapping
    public ResponseEntity<List<SchoolDTO>> getSchools() {
        return ResponseEntity.ok(schoolService.getAll());
    }

    @HasRole(RoleEnum.ADMIN)
    @PostMapping
    public ResponseEntity<SchoolDTO> createSchool(@Valid @RequestBody SchoolDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(schoolService.create(dto));
    }

    @HasRole(RoleEnum.ADMIN)
    @PutMapping("/{schoolId}")
    public ResponseEntity<SchoolDTO> updateSchool(@PathVariable Long schoolId,
                                                  @Valid @RequestBody SchoolDTO dto) {
        return ResponseEntity.ok(schoolService.update(schoolId, dto));
    }

    @HasRole(RoleEnum.ADMIN)
    @PostMapping("/{schoolId}/curators")
    public ResponseEntity<CuratorDTO> createCurator(@PathVariable Long schoolId,
                                                    @Valid @RequestBody CuratorCreateRequestDTO request)
            throws BadRequestException {
        request.setSchoolId(schoolId);
        return ResponseEntity.status(HttpStatus.CREATED).body(curatorService.create(request));
    }

    @HasRole(RoleEnum.ADMIN)
    @GetMapping("/{schoolId}/curators")
    public ResponseEntity<List<CuratorDTO>> getCurators(@PathVariable Long schoolId) {
        return ResponseEntity.ok(curatorService.getBySchoolId(schoolId));
    }
}
