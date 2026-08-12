package com.profession.suggest.controllers.pupil;

import com.profession.suggest.configuration.security.annotation.HasRole;
import com.profession.suggest.database.entities.auth.role.RoleEnum;
import com.profession.suggest.database.services.pupil.PupilService;
import com.profession.suggest.database.services.pupil.subject.PupilGradeService;
import com.profession.suggest.database.services.pupil.subject.SubjectInfoService;
import com.profession.suggest.database.services.pupil.subject.SubjectService;
import com.profession.suggest.database.services.pupil.subject.profile.PupilSubjectProfileService;
import com.profession.suggest.dto.pupil.subject.PupilSubjectDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pupil-subjects")
public class PupilSubjectsController {
    private final PupilGradeService pupilGradeService;
    private final PupilService pupilService;
    private final PupilSubjectProfileService pupilSubjectProfileService;
    private final SubjectInfoService subjectInfoService;

    public PupilSubjectsController(PupilGradeService pupilGradeService, PupilService pupilService, PupilSubjectProfileService pupilSubjectProfileService, SubjectInfoService subjectInfoService) {
        this.pupilGradeService = pupilGradeService;
        this.pupilService = pupilService;
        this.pupilSubjectProfileService = pupilSubjectProfileService;
        this.subjectInfoService = subjectInfoService;
    }

    @PostMapping("/add-subjects-info")
    @HasRole(RoleEnum.PUPIL)
    public ResponseEntity<String> addSubjectsInfo(@RequestAttribute("accountId") Long accountId, @RequestBody List<PupilSubjectDTO> pupilSubjectDTOS) {
        pupilSubjectProfileService.addSubjectProfilesForPupil(pupilSubjectDTOS, pupilService.getPupilByAccountId(accountId));
        pupilGradeService.addGradesToPupil(accountId, pupilSubjectDTOS);
        return ResponseEntity.ok("Saved");
    }
    @GetMapping
    @HasRole(RoleEnum.PUPIL)
    public ResponseEntity<List<PupilSubjectDTO>> getPupilSubjectInfo(@RequestAttribute("accountId") Long accountId) {
        return ResponseEntity.ok(subjectInfoService.getPupilSubjectsInfo(pupilService.getPupilByAccountId(accountId)));
    }
}
