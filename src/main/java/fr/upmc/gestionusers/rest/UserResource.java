package fr.upmc.gestionusers.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import fr.upmc.gestionusers.dao.PersistenceManager;
import fr.upmc.gestionusers.model.Credentials;
import fr.upmc.gestionusers.model.Role;
import fr.upmc.gestionusers.model.User;
import fr.upmc.gestionusers.security.TokenServices;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@Path("/users")
public class UserResource {
	private EntityManagerFactory emf = PersistenceManager.getInstance().getEntityManagerFactory();
	private EntityManager em;

	/**
	 * Création de compte
	 * @param user
	 * @return
	 * @throws URISyntaxException
	 */
	
	@POST
	@Path("/user")
	@Consumes("application/json")
	public Response creatUser(User user) throws URISyntaxException {
		if(user.getEmail() == null && "".equals(user.getEmail()) && user.getUserName() == null && "".equals(user.getUserName())) {
			return Response.status(Response.Status.BAD_REQUEST).entity("Username et email ne doivent pas être null ou vide").build();	
		}
		em = emf.createEntityManager();
		em.getTransaction().begin();
		
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<User> query = cb.createQuery(User.class);
		Root<User> sm = query.from(User.class);
		query.where(cb.equal(sm.get("userName"), user.getUserName()));
		List<User> verUser = em.createQuery(query).getResultList();
		if(!verUser.isEmpty())
			return Response.status(Response.Status.FORBIDDEN).entity("This user already exists").build();
		query.where(cb.equal(sm.get("email"), user.getEmail()));
		verUser = em.createQuery(query).getResultList();
		if(!verUser.isEmpty())
			return Response.status(Response.Status.FORBIDDEN).entity("This email already exists").build();
		em.persist(user);
		em.getTransaction().commit();
		em.close();
		return Response.status(Response.Status.CREATED).contentLocation(new URI(user.getFirstName())).build();
	}

	/**
	 * 
	 * @param login
	 * @return
	 * @throws URISyntaxException
	 */
	@POST
	@Path("/authentication")
	@Consumes("application/json")
	public Response signUp(Credentials credentials) throws URISyntaxException {
		String jwtToken = "";

	    if (credentials.getIdentifier() == null || credentials.getPassword() == null) {
	        throw new RuntimeException("Please fill in username and password");
	    }

	    String identifier = credentials.getIdentifier();
	    String password = credentials.getPassword();

	    em = emf.createEntityManager();
		em.getTransaction().begin();
		
		List<User> users = em.createQuery("SELECT u FROM User u WHERE u.userName = :identifier OR u.email = :identifier", User.class)
                .setParameter("identifier", identifier)
                .setMaxResults(1)
                .getResultList();
		em.getTransaction().commit();
		em.close();
		if (users.isEmpty()) {
            return Response.status(Response.Status.FORBIDDEN).entity("User not found.").build();
        }
	    User user = users.get(0);

	    String pwd = user.getPassword();

	    if (!password.equals(pwd)) {
	    	return Response.status(Response.Status.FORBIDDEN).entity("Please check your name and password.").build();
	    }

	    jwtToken = createToken(identifier, Role.USER.toString());

	    return Response.status(Response.Status.ACCEPTED).header("Authorization", "Bearer " + jwtToken).contentLocation(new URI(user.getUserName())).build();
	}
	
	private String createToken(String username, String role) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.HOUR_OF_DAY, 3);
		return TokenServices.createToken(username, role, cal.getTimeInMillis());
	}
}
