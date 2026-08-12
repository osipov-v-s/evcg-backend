package com.profession.suggest.controllers.specialist;

import com.profession.suggest.configuration.security.annotation.HasRole;
import com.profession.suggest.database.entities.auth.Account;
import com.profession.suggest.database.entities.auth.role.RoleEnum;
import com.profession.suggest.database.services.auth.AccountService;
import com.profession.suggest.database.services.profession.ProfessionService;
import com.profession.suggest.database.services.profession.ProfessionSphereService;
import com.profession.suggest.database.services.specialist.SpecialistService;
import com.profession.suggest.dto.specialist.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.security.auth.login.AccountNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

//TODO add some specialists and check if all transfer good
// - start working for auto creating a bunch of specialists from xlsx client side
@RestController
@RequestMapping("/api/specialists")
@Slf4j
public class SpecialistController {
    private static final List<String> ALLOWED_SORT_FIELDS = List.of(
            "id", "name", "surname", "experience"
    );
    private final SpecialistService specialistService;
    public final ProfessionService professionService;
    public final ProfessionSphereService professionSphereService;
    public final AccountService accountService;

    public SpecialistController(SpecialistService specialistService, ProfessionService professionService, AccountService accountService, SpecialistMapper specialistMapper, ProfessionSphereService professionSphereService) {
        this.specialistService = specialistService;
        this.professionService = professionService;
        this.accountService = accountService;
        this.professionSphereService = professionSphereService;
    }
    @HasRole(RoleEnum.ADMIN)
    @PostMapping("/register-all")
    public ResponseEntity<String> autoRegister(@RequestBody List<SpecialistRegisterRequest> specialistRegisterRequest) {
        try {
            specialistService.createAllWithAccounts(specialistRegisterRequest);
            return ResponseEntity.ok("Specialists saved");
        } catch (Exception e) {
            log.error("Error occurred while saving specialists", e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
    //TODO test it
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody SpecialistRegisterRequest specialistRegisterRequest) throws BadRequestException {
        try {
            Account account = accountService.registration(specialistRegisterRequest.getAccount(), RoleEnum.SPECIALIST);
            specialistService.create(specialistRegisterRequest.getSpecialist(), account);
            return ResponseEntity.ok(account.getEmail());
        } catch (Exception e) {
            log.error("Cannot save specialist", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }
    @HasRole({RoleEnum.ADMIN, RoleEnum.SPECIALIST})
    @PutMapping("/specialist/{id}")
    public ResponseEntity<SpecialistDTO> updateSpecialist(@RequestBody SpecialistDTO specialistDTO,
                                                           @PathVariable("id") Long id,
                                                           @RequestAttribute("accountId") Long accountId) throws AccountNotFoundException {
        specialistDTO.setId(id);
        return ResponseEntity.ok(specialistService.updateForRequester(specialistDTO, accountId));
    }
    //TODO there is a lot space for filter fields if need.
    @HasRole(RoleEnum.ADMIN)
    @GetMapping()
    public ResponseEntity<Page<SpecialistDTO>> getSpecialistsByPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String profession,
            @RequestParam(required = false) String company
    ){
        if (!ALLOWED_SORT_FIELDS.contains(sortBy))
            sortBy = "id";
        Pageable pageable = PageRequest.of(
                page, size,
                Sort.by(
                        sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC,
                        sortBy));
        try {
            return ResponseEntity.ok(specialistService.getSpecialistsPage(
                    pageable, name, profession, company));
        } catch (Exception e) {
            log.error("Error while getting specialists", e);
            throw new RuntimeException("Failed to get specialists");
        }
    }
    @HasRole(RoleEnum.ADMIN)
    @GetMapping("/reference-data")
    public ResponseEntity<List<SpecialistReferenceDTO>> getPredictionReferenceData() {
        return ResponseEntity.ok(specialistService.getPredictionReferenceData());
    }
    @GetMapping("/professions")
    public ResponseEntity<List<ProfessionDTO>> getProfessions() {
        return ResponseEntity.ok(professionService.getProfessions());
    }
    @GetMapping("/public/professions")
    public ResponseEntity<List<ProfessionDTO>> getPublicProfessions() {
        return ResponseEntity.ok(professionService.getProfessions());
    }
    @HasRole(RoleEnum.ADMIN)
    @PostMapping("/professions")
    public ResponseEntity<ProfessionDTO> createProfession(@RequestBody ProfessionDTO professionDTO) {
        return ResponseEntity.ok(professionService.createProfession(professionDTO));
    }
    @HasRole(RoleEnum.ADMIN)
    @DeleteMapping("/professions/{id}")
    public ResponseEntity<Boolean> deleteProfession(@PathVariable("id") Long id) {
        professionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/professions-spheres")
    public ResponseEntity<List<ProfessionSphereDTO>> getProfessionsSpheres() {
        return ResponseEntity.ok(professionSphereService.getProfessionsSpheres());
    }
    @HasRole(RoleEnum.ADMIN)
    @PostMapping("/professions-spheres")
    public ResponseEntity<ProfessionSphereDTO> createProfessionSphere(@RequestBody ProfessionSphereDTO professionSphereDTO) {
        return ResponseEntity.ok(professionSphereService.create(professionSphereDTO));
    }
    @HasRole(RoleEnum.ADMIN)
    @DeleteMapping("/professions-spheres/{id}")
    public ResponseEntity<Boolean> deleteProfessionSphere(@PathVariable("id") Long id) {
        professionSphereService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    @HasRole(RoleEnum.SPECIALIST)
    @GetMapping("/specialist")
    public ResponseEntity<SpecialistDTO> getSpecialist(@RequestAttribute("accountId") Long accountId) throws AccountNotFoundException {
        return ResponseEntity.ok(
                specialistService.getSpecialistByAccount(accountService.getAccountById(accountId)));
    }
    @HasRole(RoleEnum.ADMIN)
    @GetMapping("/completed-tests")
    public ResponseEntity<?> getCompletedTestsByDates(@RequestParam("startDate") LocalDate startDate,
                                                      @RequestParam("endDate") LocalDate endDate) {
        try {
            return ResponseEntity.ok(specialistService.getCompleteSpecialistsListBetween(startDate, endDate));
        }
        catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Failed to fetch specialist data",
                            "message", e.getMessage(),
                            "cause", e.getCause() != null ? e.getCause().getMessage() : "Unknown"
                    ));
        }

    }

}
