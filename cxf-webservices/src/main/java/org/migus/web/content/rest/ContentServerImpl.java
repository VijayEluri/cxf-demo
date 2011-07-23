package org.migus.web.content.rest;

import java.util.UUID;

import javax.ws.rs.WebApplicationException;
import javax.xml.datatype.DatatypeConfigurationException;

import org.migus.web.data.ContentDao;
import org.migus.web.data.ContentExistsException;
import org.migus.web.data.ContentNotFoundException;
import org.migus.web.data.ContentDao.ContentData;
import org.migus.web.content.ContentBuilder;
import org.migus.web.content.rest.ContentServer;
import org.migus.web.content.types.Content;
import org.migus.web.content.types.Contents;
import org.migus.web.content.types.NewContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ContentServerImpl implements ContentServer {
	private ContentDao contentDao;
	private Logger logger;
	
	Content convertContentData(ContentData contentData) {
		Content content=null;
		String message="converting contentData to content";
		
		logger.debug(message);
		try {
			content=ContentBuilder.fromContentData(contentData).build();
		} catch (DatatypeConfigurationException e) {
			logger.error(message, e);
			throw new WebApplicationException(500);
		}
		logger.debug("content = "+content);
		return content;
	}
	
	public ContentServerImpl(ContentDao contentDao, Logger logger) {
		this.contentDao=contentDao;
		this.logger=logger;
	}

	@Autowired
	public ContentServerImpl(ContentDao contentDao) {
		this(contentDao, LoggerFactory.getLogger(ContentServerImpl.class));
	}

	public Content add(String newId, NewContent newContent) {
		String message="adding content with newId = "+newId+"; newContent ="+
				newContent;
		ContentData contentData=null;
		UUID id=UUID.fromString(newId);

		logger.debug(message);
		try {
			contentDao.insertContent(id, newContent.getAuthor(), 
					newContent.getTitle(), newContent.getText());
		} catch (ContentExistsException e) {
			assert(id.equals(e.getId()));
			contentData=contentDao.getContent(id);
			if (contentData == null) {
				logger.error("contentData is null for existing id "+id);
				throw new WebApplicationException(500);
			}
			if(!contentData.getText().equals(newContent.getText())
					|| !contentData.getAuthor().equals(newContent.getAuthor())
					|| !contentData.getTitle().equals(newContent.getTitle())) {
				throw new WebApplicationException(409);
			}
		}
		if (contentData == null) {
			contentData=contentDao.getContent(id);
		}
		
		Content content=null;
		
		if (contentData != null) {
			content = convertContentData(contentData);
		}
		logger.debug("content = "+content);
		return content;
	}

	public Contents getByAuthor(String author) {
		Contents contents=new Contents();
		String message="getting contents for author "+author;

		logger.debug(message);
		for (ContentData contentData : contentDao.getContentsByAuthor(author)) {
			contents.getContents().add(convertContentData(contentData));
		}
		logger.debug("contents.getContents().size() = "+
				contents.getContents().size());
		return contents;
	}

	public Content get(String id) {
		ContentData contentData=contentDao.getContent(UUID.fromString(id));
		String message="getting content for id "+id;

		logger.debug(message);
		if (contentData == null) {
			logger.debug("contentData was null when "+message);
			throw new WebApplicationException(404);
		}
		
		Content content=convertContentData(contentData);
		
		logger.debug("content = "+content);
		return content;
	}

	public String updateText(String id, String text) {
		ContentData content=contentDao.getContent(UUID.fromString(id));
		String message="updating text for content with id "+id;
		
		logger.debug(message);
		content.setText(text);
		try {
			contentDao.updateContent(content);
		} catch (ContentNotFoundException e) {
			logger.debug(message, e);
			throw new WebApplicationException(404);
		}
		return text;
	}

	public String updateTitle(String id, String title) {
		ContentData content=contentDao.getContent(UUID.fromString(id));
		String message="updating title for content with id "+id;
		
		logger.debug(message);
		content.setTitle(title);
		try {
			contentDao.updateContent(content);
		} catch (ContentNotFoundException e) {
			logger.debug(message, e);
			throw new WebApplicationException(404);
		}
		return title;
	}
}
