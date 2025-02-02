package project.emsbackend.Service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import project.emsbackend.Model.Assignment;
import project.emsbackend.Model.User;
import project.emsbackend.Repository.AssignmentRepository;
import project.emsbackend.Repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {
    private final JWTService jwtService;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder(12);
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final AssignmentRepository assignmentRepository;

    public UserService(JWTService jwtService,
                       UserRepository userRepository,
                       AuthenticationManager authenticationManager,
                       EmailService emailService, AssignmentRepository assignmentRepository1) {
            this.jwtService = jwtService;
            this.userRepository = userRepository;
            this.authenticationManager = authenticationManager;
            this.emailService = emailService;
            this.assignmentRepository = assignmentRepository1;
    }

        public List<User> getUsers () {
            return userRepository.findAll();
        }
        public User getUserById ( long id){
            return userRepository.findById(id).orElse(new User());
        }

        public boolean addUser (User user){
            if (!userRepository.existsByEmail(user.getEmail()) && !userRepository.existsByPhone(user.getPhone())) {
                user.setUsername(user.getLastName() + user.getFirstName().charAt(0)
                        + (Integer.parseInt(String.valueOf(user.getPhone().charAt(user.getPhone().length() - 1))) + 4));
                user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));

                String token = UUID.randomUUID().toString();
                user.setMailVerificationToken(token);

                userRepository.save(user);
                String confirmationURL = "https://emsbackend-h3xs.onrender.com/verify-email?token=" + token;
                emailService.sendEmail(user.getEmail(), "Email Verification",
                        "Click link to verify your email: " + confirmationURL);
                return true;
            }
            return false;
        }
        public String validateVerificationToken (String token){
            User user = userRepository.findByMailVerificationToken(token);
            if (user == null) return "Invalid verification token";
            user.setEnabled(true);
            userRepository.save(user);
            return "Valid";
        }

        public void updateUser (User existingUser, User updatingUser){
            if (updatingUser.getFirstName() != null && !updatingUser.getFirstName().isEmpty())
                existingUser.setFirstName(updatingUser.getFirstName());
            if (updatingUser.getLastName() != null && !updatingUser.getLastName().isEmpty())
                existingUser.setLastName(updatingUser.getLastName());
            if (updatingUser.getEmail() != null && !updatingUser.getEmail().isEmpty())
                existingUser.setEmail(updatingUser.getEmail());
            if (updatingUser.getPhone() != null && !updatingUser.getPhone().isEmpty())
                existingUser.setPhone(updatingUser.getPhone());
            if (updatingUser.getUsername() != null && !updatingUser.getUsername().isEmpty())
                existingUser.setUsername(updatingUser.getUsername());
            if (updatingUser.getPassword() != null && !updatingUser.getPassword().isEmpty())
                existingUser.setPassword(updatingUser.getPassword());
            if (!updatingUser.getRole().equals("USER"))
                existingUser.setRole(updatingUser.getRole());
            if (updatingUser.getProfession() != null && !updatingUser.getProfession().isEmpty())
                existingUser.setProfession(updatingUser.getProfession());
            if (updatingUser.getAssignments() != null && !updatingUser.getAssignments().isEmpty()) {
                existingUser.setAssignments(updatingUser.getAssignments());
            }
            existingUser.setLocked(updatingUser.isLocked());
            existingUser.setEnabled(updatingUser.isEnabled());
            existingUser.setCredentialsExpired(updatingUser.isCredentialsExpired());
            existingUser.setExpired(updatingUser.isExpired());
            userRepository.save(existingUser);
        }

        public void deleteUser ( long id){
            User user = userRepository.findById(id).orElse(new User());
            for (Assignment assignment : user.getAssignments()) {
                assignment.getUsers().remove(user);
                assignmentRepository.save(assignment);
            }
            userRepository.deleteById(id);
        }

        public String verify (User user){
            Authentication authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword()));
            if (authentication.isAuthenticated())
                return jwtService.generateToken(user.getEmail());
            return "Failed to authenticate";
        }


        public List<Assignment> getAssignmentById ( long userId){
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                return user.getAssignments();
            } else return new ArrayList<>();
        }
    }
