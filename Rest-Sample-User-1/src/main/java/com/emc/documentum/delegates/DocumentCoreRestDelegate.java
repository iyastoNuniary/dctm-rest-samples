package com.emc.documentum.delegates;

import java.util.ArrayList;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.emc.documentum.constants.DocumentumProperties;
import com.emc.documentum.dtos.DocumentCreation;
import com.emc.documentum.dtos.DocumentumDocument;
import com.emc.documentum.dtos.DocumentumFolder;
import com.emc.documentum.dtos.DocumentumObject;
import com.emc.documentum.dtos.DocumentumProperty;
import com.emc.documentum.exceptions.CabinetNotFoundException;
import com.emc.documentum.exceptions.CanNotDeleteFolderException;
import com.emc.documentum.exceptions.DocumentCheckinException;
import com.emc.documentum.exceptions.DocumentCheckoutException;
import com.emc.documentum.exceptions.DocumentCreationException;
import com.emc.documentum.exceptions.DocumentNotFoundException;
import com.emc.documentum.exceptions.DocumentumException;
import com.emc.documentum.exceptions.FolderCreationException;
import com.emc.documentum.exceptions.RepositoryNotAvailableException;
import com.emc.documentum.wrappers.corerest.DctmRestClientX;
import com.emc.documentum.wrappers.corerest.model.JsonObject;
import com.emc.documentum.wrappers.corerest.model.PlainRestObject;
import com.emc.documentum.wrappers.corerest.util.RestTransformation;

@Component("DocumentCoreRestDelegate")
public class DocumentCoreRestDelegate implements DocumentumDelegate {

	@Autowired @Lazy
	DctmRestClientX restClientX;

	@Override
	public String getIdentifier() {
		return "rest";
	}

	@Override
	public DocumentumFolder createFolder(String cabinetName, String folderName) throws FolderCreationException,
			CabinetNotFoundException, RepositoryNotAvailableException, DocumentumException {
		JsonObject cabinet = restClientX.getObjectByPath(cabinetName);
		try {
			return RestTransformation.convertJsonObject(restClientX
					.createFolder((String) cabinet.getPropertyByName(DocumentumProperties.OBJECT_ID), folderName),
					DocumentumFolder.class);
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
			throw new DocumentumException("Unable to instantiate class of type " + DocumentumDocument.class.getName());
		}

	}

	@Override
	public DocumentumDocument createDocument(DocumentCreation docCreation) throws DocumentumException {
		JsonObject folder = restClientX.getObjectById(docCreation.getParentId());
		byte[] data = "".getBytes();
		try {
			return RestTransformation.convertJsonObject(
					restClientX.createContentfulDocument(folder, data, "FileName", "MimeType"),
					DocumentumDocument.class);
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
			throw new DocumentCreationException(
					"Unable to instantiate class of type " + DocumentumDocument.class.getName());
		}

	}

	@Override
	public DocumentumFolder getCabinetByName(String cabinetName)
			throws CabinetNotFoundException, RepositoryNotAvailableException, DocumentumException {

		return (DocumentumFolder) RestTransformation.convertJsonObject(restClientX.getObjectByPath("/" + cabinetName));
	}

	@Override
	public DocumentumObject getObjectById(String objectId)
			throws CabinetNotFoundException, RepositoryNotAvailableException {
		return RestTransformation.convertJsonObject(restClientX.getObjectById(objectId));
	}

	@Override
	public ArrayList<DocumentumFolder> getAllCabinets() throws RepositoryNotAvailableException {
		return this.getAllCabinets(1, 20);
	}

	@Override
	public ArrayList<DocumentumObject> getChildren(String folderId) throws RepositoryNotAvailableException {
		return this.getChildren(folderId, 1, 20);
	}

	@Override
	public ArrayList<DocumentumObject> getChildren(String folderId, int pageNumber, int pageSize)
			throws RepositoryNotAvailableException {
		return RestTransformation
				.convertCoreRSEntryList(restClientX.getChildrenByObjectId(folderId, pageNumber, pageSize));
	}

	@Override
	public byte[] getDocumentContentById(String documentId)
			throws DocumentNotFoundException, RepositoryNotAvailableException {
		return restClientX.getContentById(documentId,true).getData();
	}

	@Override
	public ArrayList<DocumentumObject> getDocumentByName(String name) throws RepositoryNotAvailableException {
		return RestTransformation.convertCoreRSEntryList(restClientX.queryMultipleObjectsByName("%" + name + "%"));

	}

	@Override
	public DocumentumDocument checkoutDocument(String documentId)
			throws RepositoryNotAvailableException, DocumentCheckoutException, DocumentumException {
		try {
			return RestTransformation.convertJsonObject(restClientX.checkout(documentId), DocumentumDocument.class);
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
			throw new DocumentumException("Unable to instantiate class of type " + DocumentumDocument.class.getName());
		}
	}

	@Override
	public DocumentumDocument checkinDocument(String documentId, byte[] content)
			throws RepositoryNotAvailableException, DocumentCheckinException, DocumentumException {
		try {
			return RestTransformation.convertJsonObject(restClientX.checkinDocument(documentId, content),
					DocumentumDocument.class);
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
			throw new DocumentumException("Unable to instantiate class of type " + DocumentumDocument.class.getName());
		}

	}

	@Override
	public DocumentumFolder createFolderByParentId(String parentId, String folderName)
			throws FolderCreationException, RepositoryNotAvailableException, DocumentumException {
		try {
			JsonObject folder = restClientX.createFolder(parentId, folderName);
			return RestTransformation.convertJsonObject(folder, DocumentumFolder.class);
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
			throw new DocumentumException("Unable to instantiate class of type " + DocumentumDocument.class.getName());
		}
	}

	@Override
	public ArrayList<DocumentumFolder> getAllCabinets(int pageNumber, int pageSize)
			throws RepositoryNotAvailableException {
		return RestTransformation.convertCoreRSEntryList(restClientX.getAllCabinets(pageNumber, pageSize),
				DocumentumFolder.class);
	}

	@Override
	public void deleteObject(String objectId, boolean deleteChildrenOrNot) throws CanNotDeleteFolderException {
		restClientX.deleteObjectById(objectId, deleteChildrenOrNot);

	}

	@Override
	public DocumentumObject cancelCheckout(String documentId)
			throws RepositoryNotAvailableException, DocumentCheckoutException {
		JsonObject document = restClientX.cancelCheckout(documentId);
		try {
			return RestTransformation.convertJsonObject(document, DocumentumDocument.class);
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
			throw new DocumentCheckoutException(
					"Unable to instantiate class of type " + DocumentumDocument.class.getName());
		}

	}

	@Override
	public DocumentumFolder createFolder(String parentId, HashMap<String, Object> properties)
			throws FolderCreationException, CabinetNotFoundException, RepositoryNotAvailableException,
			DocumentumException {
		JsonObject folder = restClientX.createFolder(parentId,
				(String) properties.get(DocumentumProperties.OBJECT_NAME));
		try {
			return RestTransformation.convertJsonObject(folder, DocumentumFolder.class);
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
			throw new DocumentumException("Unable to instantiate class of type " + DocumentumDocument.class.getName());
		}
	}

	@Override
	public DocumentumDocument createDocument(String parentId, DocumentumDocument docCreation)
			throws DocumentCreationException, RepositoryNotAvailableException {
		JsonObject folder = restClientX.getObjectById(parentId);
		byte[] data = "".getBytes();
		try {
			return RestTransformation.convertJsonObject(
					restClientX.createContentfulDocument(folder, data, "FileName","text/*" ,
							new PlainRestObject("dm_document", docCreation.getPropertiesAsMap())),
					DocumentumDocument.class);
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
			throw new DocumentCreationException(
					"Unable to instantiate class of type " + DocumentumDocument.class.getName());
		}
	}

	@Override
	public ArrayList<DocumentumProperty> getObjectProperties(String objectId) throws RepositoryNotAvailableException {
		return RestTransformation.convertJsonObject(restClientX.getObjectById(objectId)).getProperties();
	}

}