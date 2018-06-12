package fr.upmc.gestionusers.rest;


import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;

import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import fr.upmc.gestionusers.dao.PersistenceManager;
import fr.upmc.gestionusers.errorhandling.InfosMessage;
import fr.upmc.gestionusers.model.Position;
import fr.upmc.gestionusers.model.Role;
import fr.upmc.gestionusers.model.User;
import fr.upmc.gestionusers.security.Secured;

@Path("/secure")
@Singleton
public class SecureResource {
	private EntityManagerFactory emf = PersistenceManager.getInstance().getEntityManagerFactory();
	private EntityManager em;
	private List<User> listUsers;
	
	
	@SuppressWarnings("unchecked")
	@GET
	@Secured({Role.USER})
	@Path("/user")
    @Produces("application/json")
    public List<User> getAllUsers() {
		em = emf.createEntityManager();
		em.getTransaction().begin();
		listUsers = em.createQuery("SELECT u FROM User u").getResultList();
		em.getTransaction().commit();
	    em.close();
	    return listUsers;
    }
	
	/*
	 * Rechercher
	 */
	@GET
	@Secured({Role.USER})
	@Path("/user/{search}")
	@Produces("application/json")
	public Response findUser(@PathParam("search") String username) throws URISyntaxException {
		em = emf.createEntityManager();
		em.getTransaction().begin();
		/*aziz
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<User> query = cb.createQuery(User.class);
		Root<User> sm = query.from(User.class);
		query.where(cb.equal(sm.get("userName"), username));
		 */
		
		Query query = em.createQuery("Select u from User u where u.userName=:arg1");
		query.setParameter("arg1", username);
		User user = null;
		List result = query.getResultList();
		em.getTransaction().commit();
	    em.close();
	    if (!result.isEmpty()) {
	    	user = (User)result.get(0);
	    } else {
            return Response.status(Response.Status.FORBIDDEN).entity(new InfosMessage(Response.Status.FORBIDDEN.getStatusCode(), "This user not exists: " + username)).build();
        }
		return Response
                .status(200)
                .entity(user)
                .contentLocation(new URI("/user-management/"+username)).build();
	}
	
	@POST
	@Path("/user/{id}/position")
	@Secured({Role.USER})
	@Consumes("application/json")
	public Response miseAJourPosition(@PathParam("id") Integer id, Position position) throws URISyntaxException {
		em = emf.createEntityManager();
		em.getTransaction().begin();
		User user = em.find(User.class, id);
		if(user == null)
			return Response.status(Response.Status.FORBIDDEN).entity(new InfosMessage(Response.Status.FORBIDDEN.getStatusCode(), "This user not exists")).build();
		
		if(user.getPosition() != null) {
			user.getPosition().setLatitude(position.getLatitude());
			user.getPosition().setLongitude(position.getLongitude());
			user.getPosition().setLastUpdate(new Date());
		} else {
			position.setLastUpdate(new Date());
			position.setUser(user);
			user.setPosition(position);
		}
		
		em.merge(user);
		em.getTransaction().commit();
	    em.close();
		return Response.status(Response.Status.OK).contentLocation(new URI(user.getUserName())).build();
	} 
	
	@GET
	@Secured({Role.USER})
	@Path("/user/{id}/friends")
	@Consumes("application/json")
	public Response recupererAmis(@PathParam("id") Integer id) throws URISyntaxException {
		em = emf.createEntityManager();
		em.getTransaction().begin();
		User user = em.find(User.class, id);
		if(user == null)
			return Response.status(Response.Status.FORBIDDEN).entity(new InfosMessage(Response.Status.FORBIDDEN.getStatusCode(), "This user not exists")).build();
		em.getTransaction().commit();
	    em.close();
		return Response.status(Response.Status.OK).entity(user.getFriends()).build();
	}
	
	/*
	 * Récupérer le profil et la position d’un ami
	 */
	@GET
	@Secured({Role.USER})
	@Path("/user/{id}/friends/{friendId}")
	@Produces("application/json")
	public Response profilPositionAmi(@PathParam("id") Integer id, @PathParam("friendId") Integer friendId) throws URISyntaxException {
		em = emf.createEntityManager();
		em.getTransaction().begin();
		User user = em.find(User.class, id);
		if(user == null)
			return Response.status(Response.Status.FORBIDDEN).entity(new InfosMessage(Response.Status.FORBIDDEN.getStatusCode(), "This user not exists")).build();

		User friend = em.find(User.class, friendId);
		if(friend == null)
			return Response.status(Response.Status.FORBIDDEN).entity(new InfosMessage(Response.Status.FORBIDDEN.getStatusCode(), "This friend not exists")).build();
		
		if(!user.getFriends().contains(friend))
			return Response.status(Response.Status.FORBIDDEN).entity(new InfosMessage(Response.Status.FORBIDDEN.getStatusCode(), friendId + " is not a friend.")).build();
		em.getTransaction().commit();
	    em.close();
		return Response.status(Response.Status.OK).entity(friend).build();
	}
	
	/*
	 * Envoyer une invitation
	 */
	@POST
	@Secured({Role.USER})
	@Path("/user/{id}/sendinvitation/{recipientId}")
	@Produces("application/json")
	public Response envoyerInvitation(@PathParam("id") Integer id, @PathParam("recipientId") Integer recipientId) throws URISyntaxException {
		em = emf.createEntityManager();
		em.getTransaction().begin();
		/*Vérifier si l'utillsateur existe*/
		User user = em.find(User.class, id);
		if(user == null)
			return Response.status(Response.Status.FORBIDDEN).entity(new InfosMessage(Response.Status.FORBIDDEN.getStatusCode(), "This user not exists")).build();

		/*Vérifier si l'utillsateur qu'on veut inviter existe*/
		User recipient = em.find(User.class, recipientId);
		if(recipient == null)
			return Response.status(Response.Status.FORBIDDEN).entity(new InfosMessage(Response.Status.FORBIDDEN.getStatusCode(), "This friend not exists")).build();
		
		/*verifier s'ils ne sont pas déjà amis*/
		if(user.getFriends().contains(recipient))
			return Response.status(Response.Status.FORBIDDEN).entity(new InfosMessage(Response.Status.FORBIDDEN.getStatusCode(), recipientId + " is already a friend.")).build();
		
		/*verifier s'il n'y a pas une demande d'invitation encours*/
		if(user.getInvitationsEnvoyees().contains(recipient))
			return Response.status(Response.Status.FORBIDDEN).entity(new InfosMessage(Response.Status.FORBIDDEN.getStatusCode(), "Recipient already invited.")).build();
		
		/*On ajouter le friend dans la liste d'invités*/
		user.getInvitationsEnvoyees().add(recipient);
		/*Mise à jour*/
		em.merge(user);
		
		recipient.getInvitationsRecues().add(user);
		em.merge(recipient);
		
		em.getTransaction().commit();
	    em.close();
		return Response.status(Response.Status.OK).entity(new InfosMessage(Response.Status.OK.getStatusCode(), "Invitation Envoyée ")).build();
	}
	
	/*
	 * Envoyer une invitation
	 */
	@POST
	@Secured({Role.USER})
	@Path("/user/{id}/acceptinvitation/{friendId}")
	@Produces("application/json")
	public Response accepterInvitation(@PathParam("id") Integer id, @PathParam("friendId") Integer friendId) throws URISyntaxException {
		em = emf.createEntityManager();
		em.getTransaction().begin();
		/*Vérifier si l'utillsateur existe*/
		User user = em.find(User.class, id);
		if(user == null)
			return Response.status(Response.Status.FORBIDDEN).entity(new InfosMessage(Response.Status.FORBIDDEN.getStatusCode(), "This user not exists")).build();

		/*Vérifier si l'utillsateur qu'on veut inviter existe*/
		User sender = em.find(User.class, friendId);
		if(sender == null)
			return Response.status(Response.Status.FORBIDDEN).entity(new InfosMessage(Response.Status.FORBIDDEN.getStatusCode(), "This friend not exists")).build();
		
		/*verifier s'ils ne sont pas déjà amis*/
		if(user.getFriends().contains(sender))
			return Response.status(Response.Status.FORBIDDEN).entity(new InfosMessage(Response.Status.FORBIDDEN.getStatusCode(), friendId + " is already a friend.")).build();
		
		/*verifier s'il n'y a pas une demande d'invitation encours*/
		if(!user.getInvitationsRecues().contains(sender))
			return Response.status(Response.Status.FORBIDDEN).entity(new InfosMessage(Response.Status.FORBIDDEN.getStatusCode(), "Invitation not exists")).build();
		
		/*On ajouter le friend dans la liste d'invités*/
		user.getInvitationsRecues().remove(sender);
		user.getFriends().add(sender);
		/*Mise à jour*/
		em.merge(user);
		
		sender.getInvitationsEnvoyees().remove(user);
		sender.getFriends().add(user);
		em.merge(sender);
		
		em.getTransaction().commit();
	    em.close();
		return Response.status(Response.Status.OK).entity(new InfosMessage(Response.Status.OK.getStatusCode(), "Invitation acceptée ")).build();
	}
	
	/*
	 * Supprimer un ami
	 */
	@DELETE
	@Secured({Role.USER})
	@Path("/user/{id}/friends/{friendId}")
	public Response supprmierAmi(@PathParam("id") Integer id, @PathParam("friendId") Integer friendId) throws URISyntaxException {
		em = emf.createEntityManager();
		em.getTransaction().begin();
		User user = em.find(User.class, id);
		if(user == null)
			return Response.status(Response.Status.FORBIDDEN).entity(new InfosMessage(Response.Status.FORBIDDEN.getStatusCode(), "This user not exists")).build();

		User friend = em.find(User.class, friendId);
		if(friend == null)
			return Response.status(Response.Status.FORBIDDEN).entity(new InfosMessage(Response.Status.FORBIDDEN.getStatusCode(), "This friend not exists")).build();
		
		if(!user.getFriends().contains(friend))
			return Response.status(Response.Status.FORBIDDEN).entity(new InfosMessage(Response.Status.FORBIDDEN.getStatusCode(), friendId + " is not a friend.")).build();
		user.getFriends().remove(friend);
		em.persist(user);
		em.getTransaction().commit();
	    em.close();
		return Response.status(Response.Status.OK).build();
	} 
	
	/*
	 * Récupérer la liste des invitations envoyées
	 */
	@GET
	@Secured({Role.USER})
	@Path("/user/{id}/sentinvitation")
	@Produces("application/json")
	public Response listInvitationsEnvoyee(@PathParam("id") Integer id) throws URISyntaxException {
		em = emf.createEntityManager();
		em.getTransaction().begin();
		User user = em.find(User.class, id);
		if(user == null)
			return Response.status(Response.Status.FORBIDDEN).entity(new InfosMessage(Response.Status.FORBIDDEN.getStatusCode(), "This user not exists")).build();
		em.getTransaction().commit();
	    em.close();
		return Response.status(Response.Status.OK).entity(user.getInvitationsEnvoyees()).build();
	}
	
	/*
	 * Récupérer la liste des invitations recues
	 */
	@GET
	@Secured({Role.USER})
	@Path("/user/{id}/receivedinvitation")
	@Produces("application/json")
	public Response listInvitations(@PathParam("id") Integer id) throws URISyntaxException {
		em = emf.createEntityManager();
		em.getTransaction().begin();
		User user = em.find(User.class, id);
		if(user == null)
			return Response.status(Response.Status.FORBIDDEN).entity(new InfosMessage(Response.Status.FORBIDDEN.getStatusCode(), "This user not exists")).build();
		em.getTransaction().commit();
	    em.close();
		return Response.status(Response.Status.OK).entity(user.getInvitationsRecues()).build();
	}
	
}
