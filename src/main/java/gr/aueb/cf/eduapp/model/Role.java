package gr.aueb.cf.eduapp.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "roles")
public class Role {                                         // without extends AbstractEntity in order to avoid timestamps in this case. not needed!

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Setter(AccessLevel.NONE)
    @Getter(AccessLevel.PACKAGE)
    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY)
    private Set<User> users = new HashSet<>();

    public Set<User> getAllUsers() {
        return Set.copyOf(this.users);
    }

    @Setter(AccessLevel.NONE)
    @Getter(AccessLevel.PACKAGE)
    @ManyToMany(fetch = FetchType.LAZY)                    //This is how we treat the M:M relationships.
    @JoinTable(
            name = "roles_capabilities",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "capability_id")
    )

    private Set<Capability> capabilities = new HashSet<>();

    public Set<Capability> getAllCapabilities() {
        return Set.copyOf(capabilities);
    }


    // Helper Methods
    public void addCapability(Capability capability) {
        capabilities.add(capability);
        capability.getRoles().add(this);
    }

    public void removeCapability(Capability capability) {
        capabilities.remove(capability);
        capability.getRoles().remove(this);
    }

    public void addUser(User user) {
        users.add(user);
        user.setRole(this);
    }

    public void removeUser(User user) {
        users.remove(user);
        user.setRole(null);
    }

    public void addUsers(Collection<User> users) {              //Bulk Users addition/insert method. For many users!
        users.forEach(this::addUser);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Role role)) return false;
        return Objects.equals(getName(), role.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getName());
    }
}
