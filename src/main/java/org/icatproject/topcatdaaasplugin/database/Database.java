package org.icatproject.topcatdaaasplugin.database;

import org.icatproject.topcatdaaasplugin.Entity;
import org.icatproject.topcatdaaasplugin.EntityList;
import org.icatproject.topcatdaaasplugin.exceptions.BadRequestException;
import org.icatproject.topcatdaaasplugin.exceptions.DaaasException;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Stateless
public class Database {

    @PersistenceContext(unitName = "topcat_daaas_plugin")
    EntityManager em;

    public EntityList<Entity> query(String jpqlQuery, Map<String, Object> params) throws DaaasException {
        try {
            Integer limitPageSize = null;
            Integer limitOffset = null;

            // strip off any prefixing LIMIT options
            Pattern pattern = Pattern.compile("(?i)^(.*)LIMIT\\s+(\\d+)\\s*,\\s*(\\d+)\\s*$");
            Matcher matches = pattern.matcher(jpqlQuery);
            if (matches.find()) {
                jpqlQuery = matches.group(1);
                limitOffset = Integer.parseInt(matches.group(2));
                limitPageSize = Integer.parseInt(matches.group(3));
            }

            Query query = em.createQuery(jpqlQuery);

            for (Map.Entry<String, Object> entry : params.entrySet()) {
                query.setParameter(entry.getKey(), entry.getValue());
            }

            // re-add limit options via jpa methods
            if (limitOffset != null) {
                query.setFirstResult(limitOffset);
                query.setMaxResults(limitPageSize);
            }

            EntityList out = new EntityList<Entity>();

            for (Object entity : query.getResultList()) {
                out.add((Entity) entity);
            }

            return out;
        } catch (Exception e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    public EntityList<Entity> query(String jpqlQuery) throws DaaasException {
        return query(jpqlQuery, new HashMap<String, Object>());
    }

    public void persist(Object entity) {
        em.persist(entity);
        em.flush();
    }

    public void remove(Object entity) {
        em.remove(entity);
        em.flush();
    }

    public void detach(Object entity) {
        em.detach(entity); }

}