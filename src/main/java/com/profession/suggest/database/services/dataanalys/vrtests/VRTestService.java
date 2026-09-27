package com.profession.suggest.database.services.dataanalys.vrtests;

import com.profession.suggest.database.entities.auth.Account;
import com.profession.suggest.database.entities.dataanalys.vrtests.VRTest;
import com.profession.suggest.database.entities.dataanalys.vrtests.VRTestAnswer;
import com.profession.suggest.database.entities.dataanalys.vrtests.VRTestType;
import com.profession.suggest.database.entities.professions.Profession;
import com.profession.suggest.database.repositories.dataanalys.vrtests.VRTestRepository;
import com.profession.suggest.database.services.auth.AccountService;
import com.profession.suggest.database.services.profession.ProfessionService;
import com.profession.suggest.database.services.pupil.PupilService;
import com.profession.suggest.database.services.specialist.SpecialistService;
import com.profession.suggest.dto.dataanalys.vrtests.AnswerDTO;
import com.profession.suggest.dto.dataanalys.vrtests.VRTestDTO;
import com.profession.suggest.dto.dataanalys.vrtests.VRTestMapper;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import javax.security.auth.login.AccountNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**TODO
    1. add Param as Service
    2. Think which method required in VrTestService
    3. Think which method required in other vr services
    4. Create context vr's dtos
    5. Make controllers with getVrTests, createTest (via JWT account -> specialist/pupil)
    6. Add vrTestIntro.json and vrTestCommonQuestions.json
    7. Test all via curl

 * **/
@Service
@Slf4j
@Transactional
@AllArgsConstructor
public class VRTestService {
    private final VRTestRepository repository;
    private final VRTestAnswerService vrTestAnswerService;
    private final VRTestTypeService typeService;
    private final PupilService pupilService;
    private final SpecialistService specialistService;
    private final ProfessionService professionService;
    private final AccountService accountService;
    private final VRTestMapper mapper;
    private final static int VR_TESTS_LIMIT = 4;

    public VRTestDTO createTestWithLimitCheck(VRTestDTO dto) {
        // Validate limits based on user type
        if (dto.getPupilId() != null) {
            long count = countTestsByPupilAndProfession(dto.getPupilId(), dto.getProfessionId());
            if (count >= VR_TESTS_LIMIT) {
                throw new IllegalArgumentException("Maximum 2 tests allowed per profession.");
            }
        } else if (dto.getSpecialistId() != null) {
            long count = countTestsBySpecialistAndProfession(dto.getSpecialistId(), dto.getProfessionId());
            if (count >= VR_TESTS_LIMIT) {
                throw new IllegalArgumentException("Maximum 2 tests allowed per profession.");
            }
        } else {
            throw new IllegalArgumentException("Cannot determine who completed the test");
        }

        return createTest(dto);
    }

    public VRTestDTO createTest(VRTestDTO dto) {
        // Validations
        if (dto.getPupilId() == null && dto.getSpecialistId() == null) {
            throw new IllegalArgumentException("Cannot determine who completed the test");
        }
        if (dto.getProfessionId() == null) {
            throw new IllegalArgumentException("professionId is required");
        }
        if (dto.getTypeName() == null || dto.getTypeName().isEmpty()) {
            throw new IllegalArgumentException("typeName is required");
        }
        /*
        if (dto.getAnswers() == null || dto.getAnswers().isEmpty()) {
            throw new IllegalArgumentException("Answers are required");
        }
         */
        VRTestType type = typeService.getByName(dto.getTypeName());
        Profession profession = professionService.getProfessionById(dto.getProfessionId());
        if (type == null)
            throw new IllegalArgumentException("No such type " + dto.getTypeName());
        if (profession == null)
            throw new IllegalArgumentException("No profession found with id " + dto.getProfessionId());
        VRTest test = new VRTest();
        test.setType(type);
        test.setScore(dto.getScore());
        test.setProfession(profession);
        test.setCompletionTimeSeconds(dto.getCompletionTimeSeconds());

        if (dto.getPupilId() != null)
            test.setPupil(pupilService.getPupilById(dto.getPupilId()));
        else if (dto.getSpecialistId() != null)
            test.setSpecialist(specialistService.getSpecialistById(dto.getSpecialistId()));
        VRTest savedTest = repository.save(test);
        //TODO check if answers null all need to work
        Set<VRTestAnswer> answers = new HashSet<>();
        for (AnswerDTO answerDTO : dto.getAnswers()) {
            VRTestAnswer answer = new VRTestAnswer();
            answer.setVrTest(savedTest);
            answer.setQuestionText(answerDTO.getQuestionText());
            answer.setAnswerText(answerDTO.getAnswerText());
            answer.setAnswerScore(answerDTO.getAnswerScore());
            answers.add(answer);
        }
        vrTestAnswerService.createAll(answers);
        test.setAnswers(new HashSet<>(answers));

        return mapper.toDTO(savedTest);
    }
    public List<VRTestDTO> getAllTests() {
        return repository.findAll().stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }
    public List<VRTestDTO> getTestsByAccountId(Long accountId) {
        List<VRTest> tests = new ArrayList<>();
        tests.addAll(repository.findByPupilAccountId(accountId));
        tests.addAll(repository.findBySpecialistAccountId(accountId));

        return tests.stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }
    public List<VRTestDTO> getTestsByAccountIdAndProfessionId(Long accountId, Long professionId) throws AccountNotFoundException {
        List<VRTest> tests = new ArrayList<>();
        Account account = accountService.getAccountById(accountId);
        if (account.getPupil() != null)
            tests.addAll(repository.findByPupilIdAndProfessionId(account.getPupil().getId(), professionId));
        else if (account.getSpecialist() != null)
            tests.addAll(repository.findBySpecialistIdAndProfessionId(account.getSpecialist().getId(), professionId));
        return tests.stream().map(mapper::toDTO).collect(Collectors.toList());
    }
    public long countTestsByPupilAndProfession(Long pupilId, Long professionId) {
        return repository.countByPupilIdAndProfessionId(pupilId, professionId);
    }
    public long countTestsBySpecialistAndProfession(Long specialistId, Long professionId) {
        return repository.countBySpecialistIdAndProfessionId(specialistId, professionId);
    }
    @Transactional
    public void deleteAllTestsByAccountAndProfession(Long accountId, Long professionId) {
        // Get tests from both pupil and specialist queries
        List<VRTest> pupilTests = repository.findByPupilAccountId(accountId);
        List<VRTest> specialistTests = repository.findBySpecialistAccountId(accountId);

        // Combine and filter by profession
        List<VRTest> tests = Stream.concat(pupilTests.stream(), specialistTests.stream())
                .filter(test -> test.getProfession() != null && test.getProfession().getId().equals(professionId))
                .collect(Collectors.toList());

        if (tests.isEmpty()) {
            throw new IllegalArgumentException("No tests found for account " + accountId +
                    " with profession " + professionId);
        }
        tests.forEach(test -> test.getAnswers()
                        .forEach(vrTestAnswerService::delete));
        repository.deleteAll(tests);
        log.info("Deleted {} tests for account {} and profession {}", tests.size(), accountId, professionId);
    }

    public void prepareTestDto(VRTestDTO dto, Account account) {
        if (account.getPupil() != null) {
            dto.setPupilId(account.getPupil().getId());
            dto.setSpecialistId(null);
        } else if (account.getSpecialist() != null) {
            dto.setSpecialistId(account.getSpecialist().getId());
            dto.setPupilId(null);
        } else {
            throw new IllegalArgumentException("Invalid account type");
        }
    }

}
