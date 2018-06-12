package fr.upmc.gestionusers.model;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "USER")
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Integer id;

	@Column(name = "user_name")
	private String userName;

	@Column(name = "first_name")
	private String firstName;

	@Column(name = "last_name")
	private String lastName;

	@Column(name = "email")
	private String email;

	@Column(name = "password")
	private String password;

	@OneToOne(fetch = FetchType.EAGER, mappedBy = "user", cascade = CascadeType.ALL)
	private Position position;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "friend",
		joinColumns = @JoinColumn(name = "user_id"),
		inverseJoinColumns = @JoinColumn(name = "friend_id"),
		uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "friend_id"})})
	List<User> friends = null;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "sent_invitation",
		joinColumns = @JoinColumn(name = "user_id"),
		inverseJoinColumns = @JoinColumn(name = "recipient_id"),
		uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "recipient_id"})})
	List<User> invitationsEnvoyees = null;
	
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "received_invitation",
		joinColumns = @JoinColumn(name = "user_id"),
		inverseJoinColumns = @JoinColumn(name = "sender_id"),
		uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "sender_id"})})
	List<User> invitationsRecues = null;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public User() {
		super();
	}

	public User(String userName, String password) {
		this.userName = userName;
		this.password = password;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String username) {
		this.userName = username;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Position getPosition() {
		return position;
	}

	public void setPosition(Position position) {
		this.position = position;
	}

	@JsonIgnore
	public List<User> getFriends() {
		return friends;
	}

	public void setFriends(List<User> friends) {
		this.friends = friends;
	}

	@JsonIgnore
	public List<User> getInvitationsEnvoyees() {
		return invitationsEnvoyees;
	}

	public void setInvitationsEnvoyees(List<User> invitationsEnvoyees) {
		this.invitationsEnvoyees = invitationsEnvoyees;
	}

	@JsonIgnore
	public List<User> getInvitationsRecues() {
		return invitationsRecues;
	}

	public void setInvitationsRecues(List<User> invitationsRecues) {
		this.invitationsRecues = invitationsRecues;
	}

	@Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof User)) return false;
        User user = (User) obj;
        return user.id == id;
    }
}
