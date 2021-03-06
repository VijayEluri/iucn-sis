package org.iucn.sis.server.extensions.attachments;

import org.iucn.sis.server.api.application.SimpleSISApplication;

public class ServerApplication extends SimpleSISApplication {

	public ServerApplication() {
		super(RunMode.ONLINE);
	}

	/**
	 * Attachments are available online only.
	 */
	public void init() {
		FileAttachmentUploadRestlet upload = new FileAttachmentUploadRestlet(app.getContext());
		addResource(upload, upload.getPaths(), true);
		
		addServiceToRouter(new FileAttachmentRestlet(app.getContext()));
	}

}
