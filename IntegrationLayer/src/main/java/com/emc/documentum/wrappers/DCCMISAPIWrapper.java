package com.emc.documentum.wrappers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.QueryResult;
import org.apache.chemistry.opencmis.client.api.QueryStatement;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.runtime.OperationContextImpl;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.Action;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.UnfileObject;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConnectionException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConstraintException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.commons.io.IOUtils;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.emc.documentum.constants.DCCMISConstants;
import com.emc.documentum.exceptions.CanNotDeleteFolderException;
import com.emc.documentum.exceptions.DocumentCheckoutException;
import com.emc.documentum.exceptions.DocumentumException;
import com.emc.documentum.exceptions.ObjectNotFoundException;
import com.emc.documentum.exceptions.RepositoryNotAvailableException;
import com.emc.documentum.model.UserModel;

@Component("DCCMISAPIWrapper")
public class DCCMISAPIWrapper {

	Logger log = Logger.getLogger(this.getClass().getCanonicalName());

	@Autowired
	DCCMISConstants data;

	private Session getSession(String username, String password, String repo) {
		SessionFactory sessionFactory = SessionFactoryImpl.newInstance();
		Map<String, String> parameter = new HashMap<String, String>();
		parameter.put(SessionParameter.USER, username);
		parameter.put(SessionParameter.PASSWORD, password);
		parameter.put(SessionParameter.ATOMPUB_URL, "http://" + data.host + ":" + data.port + "/emc-cmis/resources");
		System.out.println(parameter.get(SessionParameter.ATOMPUB_URL));
		parameter.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
		parameter.put(SessionParameter.REPOSITORY_ID, repo);
		Session session = sessionFactory.createSession(parameter);
		return session;
	}

	public UserModel getUserInfo(String username, String password) {
		UserModel user = new UserModel();

		if (getSession(username, password, data.repo) != null) {
			user.setName(username);
		}

		return user;
	}

	public Document createDocument(Folder folder, HashMap<String, Object> properties)
			throws RepositoryNotAvailableException {
		try {
			return folder.createDocument(properties, null, null);
		} catch (CmisConnectionException e) {
			throw new RepositoryNotAvailableException("CMIS", e);
		}
	}

	public Folder getFolderByPath(String queryFolderPath)
			throws ObjectNotFoundException, RepositoryNotAvailableException {
		try {
			return (Folder) (getSession(data.username, data.password, data.repo)).getObjectByPath(queryFolderPath);
		} catch (CmisConnectionException e) {
			throw new RepositoryNotAvailableException("CMIS", e);
		}
	}

	public ArrayList<CmisObject> getAllCabinets(int pageNumber, int pageSize) throws RepositoryNotAvailableException {
		try {
			Session session = getSession(data.username, data.password, data.repo);
			Folder rootFolder = session.getRootFolder();
			ItemIterable<CmisObject> children = rootFolder.getChildren();
			ArrayList<CmisObject> navigationObjects = new ArrayList<>();
			for (CmisObject o : children.skipTo(--pageNumber * pageSize).getPage(pageSize)) {
				navigationObjects.add(o);
			}
			return navigationObjects;
		} catch (CmisConnectionException e) {
			throw new RepositoryNotAvailableException("CMIS", e);
		}
	}

	public ArrayList<CmisObject> getChildren(String folderId, int pageNumber, int pageSize)
			throws DocumentumException {
		try {
			Session session = getSession(data.username, data.password, data.repo);
			CmisObject object = session.getObject(folderId);
			if(!object.getBaseType().getLocalName().equals("dm_folder"))
			{
				throw new DocumentumException(folderId+ " is not a folder.");
			}
			Folder rootFolder = (Folder) object;
			ItemIterable<CmisObject> children = rootFolder.getChildren();
			ArrayList<CmisObject> navigationObjects = new ArrayList<>();
			for (CmisObject o : children.skipTo(--pageNumber * pageSize).getPage(pageSize)) {
				navigationObjects.add(o);
			}
			return navigationObjects;
		} catch (CmisConnectionException e) {
			throw new RepositoryNotAvailableException("CMIS", e);
		}
	}

	public byte[] getDocumentContentById(String documentId)
			throws ObjectNotFoundException, RepositoryNotAvailableException {
		try {
			Session session = getSession(data.username, data.password, data.repo);
			CmisObject object = session.getObject(documentId);
			if(!object.getBaseType().getLocalName().equals("dm_document"))
			{
				throw new ObjectNotFoundException(documentId +" is not a document.");
			}
			Document document = (Document) object;

			System.out.println(document.getContentStreamLength());
			System.out.println(document.getContentStream().getLength());
			byte[] fileContent = IOUtils.toByteArray(document.getContentStream().getStream());
			byte[] encodedfile = Base64.encodeBase64(fileContent);
			System.out.println(encodedfile);
			return encodedfile;

		} catch (CmisConnectionException e) {
			throw new RepositoryNotAvailableException("CMIS", e);
		} catch (IOException e) {
			log.log(Level.SEVERE, "IO Exception while reading dm_document input stream", e);
			return new byte[0];
		}
	}

	public Folder createFolder(Folder folder, Map<String, ?> properties) throws RepositoryNotAvailableException {
		try {
			return folder.createFolder(properties);
		} catch (CmisConnectionException e) {
			throw new RepositoryNotAvailableException("CMIS", e);
		}
	}

	public Folder createFolder(Folder folder, String folderName) throws RepositoryNotAvailableException {
		try {
			HashMap<String, Object> properties = new HashMap<String, Object>();
			properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:folder");
			properties.put(PropertyIds.NAME, folderName);
			return createFolder(folder, properties);
		} catch (CmisConnectionException e) {
			throw new RepositoryNotAvailableException("CMIS", e);
		}
	}

	public void deleteObject(CmisObject object, boolean deleteChildrenOrNot)
			throws RepositoryNotAvailableException, CanNotDeleteFolderException {
		try {
			if (object.getType().getLocalName().equals("dm_folder") && deleteChildrenOrNot) {
				((Folder) object).deleteTree(true, UnfileObject.DELETE, true);
			} else {
				object.delete();
			}
		} catch (CmisConnectionException e) {
			throw new RepositoryNotAvailableException("CMIS", e);
		} catch (CmisConstraintException ex) {
			ex.printStackTrace();
			throw new CanNotDeleteFolderException(object.getId());
		}
	}

	public CmisObject getObjectById(String cabinetId) throws RepositoryNotAvailableException {
		try {
			return getSession(data.username, data.password, data.repo).getObject(cabinetId);
		} catch (CmisConnectionException e) {
			throw new RepositoryNotAvailableException("CMIS", e);
		}
	}

	public ItemIterable<QueryResult> getObjectsByName(String name) throws RepositoryNotAvailableException {
		try {
			String queryString = String.format("Select F.* FROM cmis:folder F where cmis:name like '%s'",
					"%" + name + "%");
			log.info("Executing Query " + queryString);
			QueryStatement queryStatement = getSession(data.username, data.password, data.repo)
					.createQueryStatement(queryString);

			OperationContext operationContext = new OperationContextImpl();
			operationContext.setMaxItemsPerPage(20);
			ItemIterable<QueryResult> queryResult = queryStatement.query(false, operationContext);
			return queryResult;
		} catch (CmisConnectionException e) {
			throw new RepositoryNotAvailableException("CMIS", e);
		}
	}

	public Document checkoutDocument(String documentId) throws DocumentCheckoutException, RepositoryNotAvailableException {
		try {
			Session session = getSession(data.username, data.password, data.repo);

			Document document = (Document) session.getObject(documentId);
			AllowableActions actions = document.getAllowableActions();
			boolean canCheckout = actions.getAllowableActions().contains(Action.CAN_CHECK_OUT);
			if (!canCheckout) {
				throw new DocumentCheckoutException("document already checked out");
			}
			Document checkoutDocument = (Document) session.getObject(document.checkOut());
			return checkoutDocument;
		} catch (CmisConnectionException e) {
			throw new RepositoryNotAvailableException("CMIS", e);
		}
	}

	public Document cancelCheckout(String documentId) throws DocumentCheckoutException, RepositoryNotAvailableException {
		try {
			Session session = getSession(data.username, data.password, data.repo);
			Document document = (Document) session.getObject(documentId);
			AllowableActions actions = document.getAllowableActions();
			boolean canCancelCheckout = actions.getAllowableActions().contains(Action.CAN_CANCEL_CHECK_OUT);
			if (!canCancelCheckout) {
				throw new DocumentCheckoutException("document is not Checked out");
			}
			document.cancelCheckOut();
			return document;
		} catch (CmisConnectionException e) {
			throw new RepositoryNotAvailableException("CMIS", e);
		}
	}

	public Document checkinDocument(String documentId, byte[] content) throws RepositoryNotAvailableException {
		try {
			Session session = getSession(data.username, data.password, data.repo);
			Document document = (Document) session.getObject(documentId);
			ContentStream contentStream = session.getObjectFactory().createContentStream(
					document.getContentStream().getFileName(), content.length,
					document.getContentStream().getMimeType(), new ByteArrayInputStream(Base64.decodeBase64(content)));
			ObjectId newDocumentId = document.checkIn(true, null, contentStream, "Major version");
			return (Document) session.getObject(newDocumentId);
		} catch (CmisConnectionException e) {
			throw new RepositoryNotAvailableException("CMIS", e);
		}
	}
	public CmisObject renameObject(String objectId,String newName) throws RepositoryNotAvailableException, ObjectNotFoundException
	{
		try {
			Session session = getSession(data.username, data.password, data.repo);
			CmisObject object = session.getObject(objectId);
			object.updateProperties(Collections.<String,String>singletonMap("cmis:name", newName));
			return session.getObject(objectId);
		} catch (CmisConnectionException e) {
			throw new RepositoryNotAvailableException("CMIS", e);
		}catch (CmisRuntimeException e) {
			throw new ObjectNotFoundException(objectId + " not found.");
		}
	}
	public CmisObject updateObject(String objectId,Map<String,Object> newProperties) throws RepositoryNotAvailableException, ObjectNotFoundException
	{
		try {
			Session session = getSession(data.username, data.password, data.repo);
			CmisObject object = session.getObject(objectId);
			object.updateProperties(newProperties);
			return session.getObject(objectId);
		} catch (CmisConnectionException e) {
			throw new RepositoryNotAvailableException("CMIS", e);
		}catch (CmisRuntimeException e) {
			throw new ObjectNotFoundException(objectId + " not found.");
		}
	}
	public CmisObject moveObject(String objectId,String targetFolderId) throws DocumentumException
	{
		try {
			Session session = getSession(data.username, data.password, data.repo);
			CmisObject object = session.getObject(objectId);
			CmisObject targetFolder = session.getObject(targetFolderId);
			if(!targetFolder.getBaseType().getLocalName().equals("dm_folder"))
			{
				throw new DocumentumException(targetFolderId + " is not a folder.");
			}
			if(object.getBaseType().getLocalName().equals("dm_folder"))
			{
				Folder folder =(Folder)object;
				return folder.move(session.createObjectId(folder.getParentId()), session.createObjectId(targetFolder.getId()));
			}
			else if(object.getBaseType().getLocalName().equals("dm_document"))
			{
				Document document =(Document)object;
				return document.move(session.createObjectId(document.getParents().get(0).getId()), session.createObjectId(targetFolder.getId()));
			}
			return session.getObject(objectId);
		} catch (CmisConnectionException e) {
			throw new RepositoryNotAvailableException("CMIS", e);
		}catch (CmisRuntimeException e) {
			throw new ObjectNotFoundException(objectId + " not found.");
		}
	}
	public CmisObject copyObject(String objectId,String targetFolderId) throws DocumentumException
	{
		try {
			Session session = getSession(data.username, data.password, data.repo);
			CmisObject object = session.getObject(objectId);
			CmisObject targetFolder = session.getObject(targetFolderId);
			if(!targetFolder.getBaseType().getLocalName().equals("dm_folder"))
			{
				throw new DocumentumException(targetFolderId + " is not a folder.");
			}
			if(object.getBaseType().getLocalName().equals("dm_folder"))
			{
				Folder folder =(Folder)object;
				return copyFolder(folder, (Folder)targetFolder);
			}
			else if(object.getBaseType().getLocalName().equals("dm_document"))
			{
				Document document =(Document)object;
				return document.copy(targetFolder);
			}
			return session.getObject(objectId);
		} catch (CmisConnectionException e) {
			throw new RepositoryNotAvailableException("CMIS", e);
		}catch (CmisRuntimeException e) {
			throw new ObjectNotFoundException(objectId + " not found.");
		}
	}
	private Folder copyFolder(Folder sourceFolder,Folder targetFolder) throws ObjectNotFoundException, RepositoryNotAvailableException
	{
		try{
			Map<String,Object> folderProperties = new HashMap<String,Object>();
			folderProperties.put(PropertyIds.NAME, sourceFolder.getPropertyValue(PropertyIds.NAME));
			folderProperties.put(PropertyIds.OBJECT_TYPE_ID, sourceFolder.getPropertyValue(PropertyIds.OBJECT_TYPE_ID));
			Folder createdFolder = targetFolder.createFolder(folderProperties);
			ItemIterable<CmisObject>children =  sourceFolder.getChildren();
			for (CmisObject cmisObject : children) {
				if(cmisObject.getBaseType().getLocalName().equals("dm_document"))
				{
					((Document)cmisObject).copy(createdFolder);
				}
				else
				{
					copyFolder((Folder)cmisObject, createdFolder);
				}
			}
			return createdFolder;
		} catch (CmisConnectionException e) {
			throw new RepositoryNotAvailableException("CMIS", e);
		}catch (CmisRuntimeException e) {
			throw new ObjectNotFoundException(sourceFolder.getId() + " not found.");
		}
	}
}
