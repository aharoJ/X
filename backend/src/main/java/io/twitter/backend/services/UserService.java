package io.twitter.backend.services;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.twitter.backend.models.Role;
import io.twitter.backend.exeptions.EmailAlreadyTakenExeption;
import io.twitter.backend.exeptions.EmailFailedToSendException;
import io.twitter.backend.exeptions.IncorrectVerificationCodeException;
import io.twitter.backend.exeptions.UserDoesNotExistException;
import io.twitter.backend.models.ApplicationUser;
import io.twitter.backend.models.RegistrationObject;
import io.twitter.backend.repositories.RoleRepository;
import io.twitter.backend.repositories.UserRepository;

@Service
public class UserService {
  private final UserRepository userRepo;
  private final RoleRepository roleRepo;
  private final MailService mailService;

  @Autowired
  public UserService(UserRepository userRepo, RoleRepository roleRepo, MailService mailService) {
    this.userRepo = userRepo;
    this.roleRepo = roleRepo;
    this.mailService = mailService;
  }

  public ApplicationUser getUserByUsername(String username) {
    return userRepo.findByUsername(username).orElseThrow(UserDoesNotExistException::new);
  }

  public ApplicationUser updateUser(ApplicationUser user) {
    try {
      return userRepo.save(user);
    } catch (Exception e) {
      throw new EmailAlreadyTakenExeption();
    }
  }

  public ApplicationUser registerUser(RegistrationObject ro) {
    ApplicationUser user = new ApplicationUser();

    user.setFirstName(ro.getFirstName());
    user.setLastName(ro.getLastName());
    user.setEmail(ro.getEmail());
    user.setDateOfBirth(ro.getDob());

    String name = user.getFirstName() + user.getLastName();

    boolean nameTaken = true;

    String tempName = "";
    while (nameTaken) {
      tempName = generateUsername(name);

      if (userRepo.findByUsername(name).isEmpty()) {
        nameTaken = false;
      }
    }

    user.setUsername(tempName);

    Set<Role> roles = user.getAuthorities();
    roles.add(roleRepo.findRoleByAuthority("USER").get());
    user.setAuthorities(roles);

    try {
      return userRepo.save(user);
    } catch (Exception e) {
      throw new EmailAlreadyTakenExeption();
    }
  }

  public void generateEmailVerification(String username) {
    ApplicationUser user = userRepo.findByUsername(username).orElseThrow(UserDoesNotExistException::new);

    user.setVerification(generateVerificationNumber());

    try {
      mailService.sendEmail(user.getEmail(), "subject~ Your verification code",
          "content~ Here is your verification code: " + user.getVerification());
      userRepo.save(user);
    } catch (Exception e) {
      throw new EmailFailedToSendException();
    }
    userRepo.save(user);

  }

  public ApplicationUser verifyEmail(String username, Long code) {
    ApplicationUser user = userRepo.findByUsername(username).orElseThrow(UserDoesNotExistException::new);

    if (code.equals(user.getVerification())) {
      user.setEnabled(true);
      user.setVerification(null);

      return userRepo.save(user);
    } else {
      throw new IncorrectVerificationCodeException();
    }

  }

  private String generateUsername(String name) {
    long generatedNumber = (long) Math.floor(Math.random() * 1_000_000_000);
    return name + generatedNumber;

  }

  private Long generateVerificationNumber() {

    return (long) Math.floor(Math.random() * 100_000_000);
  }

}
