package org.iucn.sis.client.components.tabs;

import java.util.ArrayList;
import java.util.List;

import org.iucn.sis.client.acl.AuthorizationCache;
import org.iucn.sis.client.components.ClientUIContainer;
import org.iucn.sis.client.components.panels.NewAssessmentPanel;
import org.iucn.sis.client.components.panels.PanelManager;
import org.iucn.sis.client.components.panels.TaxonTreePopup;
import org.iucn.sis.client.components.panels.TaxonomyBrowserPanel.TaxonListElement;
import org.iucn.sis.client.data.assessments.AssessmentCache;
import org.iucn.sis.client.simple.SimpleSISClient;
import org.iucn.sis.client.taxomatic.CreateNewTaxonPanel;
import org.iucn.sis.client.taxomatic.LateralMove;
import org.iucn.sis.client.taxomatic.MergePanel;
import org.iucn.sis.client.taxomatic.MergeUpInfrarank;
import org.iucn.sis.client.taxomatic.SplitNodePanel;
import org.iucn.sis.client.taxomatic.TaxomaticAssessmentMover;
import org.iucn.sis.client.taxomatic.TaxomaticDemotePanel;
import org.iucn.sis.client.taxomatic.TaxomaticPromotePanel;
import org.iucn.sis.client.taxomatic.TaxonBasicEditor;
import org.iucn.sis.client.taxomatic.TaxonChooser;
import org.iucn.sis.client.taxomatic.TaxonCommonNameEditor;
import org.iucn.sis.client.taxomatic.TaxonSynonymEditor;
import org.iucn.sis.shared.acl.base.AuthorizableObject;
import org.iucn.sis.shared.acl.feature.AuthorizableFeature;
import org.iucn.sis.shared.data.TaxonomyCache;
import org.iucn.sis.shared.data.assessments.AssessmentData;
import org.iucn.sis.shared.taxonomyTree.TaxonNode;

import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.MenuEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.Info;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.TabItem;
import com.extjs.gxt.ui.client.widget.Window;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.FillLayout;
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.extjs.gxt.ui.client.widget.menu.MenuItem;
import com.extjs.gxt.ui.client.widget.toolbar.SeparatorToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.solertium.lwxml.shared.GenericCallback;
import com.solertium.lwxml.shared.NativeDocument;
import com.solertium.util.extjs.client.WindowUtils;

public class TaxonHomePageTab extends TabItem {
	private PanelManager panelManager = null;
	private ToolBar toolBar = null;
	private Button assessmentTools;
	private Button taxonToolsItem;
	private Button taxomaticToolItem;

	private final boolean ENABLE_TAXOMATIC_FEATURES = true;

	/**
	 * Defaults to having Style.NONE
	 */
	public TaxonHomePageTab(PanelManager manager) {
		super();
		panelManager = manager;

		build();
	}

	public void build() {
		setText("Taxon Home Page");

		BorderLayout layout = new BorderLayout();
		// layout.setSpacing( 0 );
		// layout.setMargin( 0 );
		setLayout(layout);

		toolBar = buildToolBar();
		BorderLayoutData toolbarData = new BorderLayoutData(LayoutRegion.NORTH);
		toolbarData.setSize(25);

		BorderLayoutData summaryData = new BorderLayoutData(LayoutRegion.CENTER, .5f, 100, 500);
		summaryData.setSize(300);
		BorderLayoutData browserData = new BorderLayoutData(LayoutRegion.EAST, .5f, 100, 500);
		browserData.setSize(250);

		add(panelManager.taxonomicSummaryPanel, summaryData);
		// add( panelManager.taxonomyBrowserPanel, browserData );
		add(toolBar, toolbarData);

		panelManager.taxonomicSummaryPanel.update(null);
		// panelManager.taxonomyBrowserPanel.update();

		layout();
	}

	private ToolBar buildToolBar() {
		ToolBar toolbar = new ToolBar();

		assessmentTools = new Button();
		assessmentTools.setText("Assessment Tools");
		assessmentTools.setIconStyle("icon-preferences-wrench");

		MenuItem mItem = new MenuItem();
		mItem.setText("Goto Most Recent");
		mItem.setIconStyle("icon-go-jump");
		mItem.addSelectionListener(new SelectionListener<MenuEvent>() {
			public void componentSelected(MenuEvent ce) {

			}
		});

		mItem = new MenuItem();
		mItem.setText("Assess Current Taxon");
		mItem.setIconStyle("icon-new-document");

		mItem.addSelectionListener(new SelectionListener<MenuEvent>() {
			public void componentSelected(MenuEvent ce) {
				if (TaxonomyCache.impl.getCurrentNode().getFootprint().length < TaxonNode.SPECIES) {
					WindowUtils.errorAlert("You must select a species or lower taxa to assess.  "
							+ "You can select a different taxon using the navigator, the search function, "
							+ " or the browser.");
				} else {
					Window shell = WindowUtils.getWindow(true, false, "New "
							+ TaxonomyCache.impl.getCurrentNode().getFullName() + " Assessment");
					shell.setLayout(new FillLayout());
					shell.setSize(550, 250);
					shell.add(new NewAssessmentPanel(panelManager));
					shell.show();
				}

			}
		});

		Menu mainMenu = new Menu();
		/*
		 * Menu subMenu = new Menu();
		 * 
		 * MenuItem subMItem = new MenuItem();
		 * subMItem.setIconStyle("icon-copy");
		 * subMItem.setText("Using This Data"); subMItem.addListener(
		 * Events.Select, new Listener() { public void handleEvent(BaseEvent be)
		 * { AssessmentCache.impl.createNewUserAssessment( true ); } });
		 * subMenu.add( subMItem );
		 * 
		 * subMItem = new MenuItem(); subMItem.setIconStyle("icon-copy");
		 * subMItem.setText("From Scratch"); subMItem.addListener(
		 * Events.Select, new Listener() { public void handleEvent(BaseEvent be)
		 * { AssessmentCache.impl.createNewUserAssessment( false ); } });
		 * subMenu.add( subMItem );
		 * 
		 * mItem.setSubMenu( subMenu ); mainMenu.add(mItem);
		 */
		mainMenu.add(mItem);
		assessmentTools.setMenu(mainMenu);

		toolbar.add(assessmentTools);
		toolbar.add(new SeparatorToolItem());

		taxonToolsItem = new Button();
		taxonToolsItem.setText("Taxon Tools");
		taxonToolsItem.setIconStyle("icon-preferences-wrench-orange");

		mainMenu = new Menu();
		taxonToolsItem.setMenu(mainMenu);

		// mainMenu.add( mItem );

		mItem = new MenuItem();
		mItem.setText("View/Attach Note");
		mItem.setIconStyle("icon-note");
		mItem.addSelectionListener(new SelectionListener<MenuEvent>() {
			public void componentSelected(MenuEvent ce) {
				panelManager.taxonomicSummaryPanel.buildNotePopup();
			}
		});
		mainMenu.add(mItem);

		mItem = new MenuItem();
		mItem.setText("View/Attach Reference");
		mItem.setIconStyle("icon-book");
		mItem.addSelectionListener(new SelectionListener<MenuEvent>() {
			public void componentSelected(MenuEvent ce) {
				panelManager.taxonomicSummaryPanel.buildReferencePopup();

			}
		});
		mainMenu.add(mItem);

		toolbar.add(taxonToolsItem);
		toolbar.add(new SeparatorToolItem());

		// BEGIN TAXOMATIC FEATURES
		if (ENABLE_TAXOMATIC_FEATURES) {
			taxomaticToolItem = new Button();
			taxomaticToolItem.setText("Taxomatic Tools");
			taxomaticToolItem.setIconStyle("icon-preferences-wrench-green");
			mainMenu = new Menu();
			taxomaticToolItem.setMenu(mainMenu);

			mItem = new MenuItem();
			mItem.setText("Edit Taxon");
			mItem.setIconStyle("icon-note-edit");
			mItem.addSelectionListener(new SelectionListener<MenuEvent>() {
				public void componentSelected(MenuEvent ce) {
					popupChooserPromote("Basic Taxon Information Editor", "icon-note-edit", new TaxonBasicEditor(
							panelManager));
				}
			});
			mainMenu.add(mItem);

			mItem = new MenuItem();
			mItem.setText("Edit Synonyms");
			mItem.setIconStyle("icon-note-edit");
			mItem.addSelectionListener(new SelectionListener<MenuEvent>() {
				public void componentSelected(MenuEvent ce) {
					popupChooser("Synonym Editor", "icon-note-edit", new TaxonSynonymEditor());
				}
			});
			mainMenu.add(mItem);

			mItem = new MenuItem();
			mItem.setText("Edit Common Names");
			mItem.setIconStyle("icon-note-edit");
			mItem.addSelectionListener(new SelectionListener<MenuEvent>() {
				public void componentSelected(MenuEvent ce) {
					popupChooser("Common Names Validator", "icon-note-edit", new TaxonCommonNameEditor(panelManager));
				}
			});
			mainMenu.add(mItem);

			// TODO: Decide if need to guard against deprecated nodes
			mItem = new MenuItem();
			mItem.setText("Add New Child Taxon");
			mItem.setIconStyle("icon-new-document");
			mItem.addSelectionListener(new SelectionListener<MenuEvent>() {
				public void componentSelected(MenuEvent ce) {
					TaxonNode curNode = TaxonomyCache.impl.getCurrentNode();
					if (curNode != null) {
						if( AuthorizationCache.impl.hasRight(SimpleSISClient.currentUser, AuthorizableObject.CREATE, curNode) )
							CreateNewTaxonPanel.impl.open(TaxonomyCache.impl.getCurrentNode());
						else
							WindowUtils.errorAlert("Insufficient Permission", "Sorry. You do not have create permissions for this taxon.");
					} else {
						WindowUtils.errorAlert("Please select a taxon to attach to.");
					}
				}
			});
			mainMenu.add(mItem);

			mItem = new MenuItem();
			mItem.setText("Lateral Move");
			mItem.setIconStyle("icon-lateral-move");
			mItem.addSelectionListener(new SelectionListener<MenuEvent>() {
				public void componentSelected(MenuEvent ce) {
					popupChooser("Perform Lateral Move", "icon-lateral-move", new LateralMove(panelManager));
				}
			});
			mainMenu.add(mItem);

			mItem = new MenuItem();
			mItem.setText("Promote Taxon");
			mItem.setIconStyle("icon-promote");
			mItem.addSelectionListener(new SelectionListener<MenuEvent>() {
				public void componentSelected(MenuEvent ce) {
					TaxonNode node = TaxonomyCache.impl.getCurrentNode();
					if (node != null && node.getLevel() == TaxonNode.INFRARANK)
						popupChooserPromote("Promote Taxon", "icon-promote", new TaxomaticPromotePanel(panelManager));
					else
						WindowUtils.infoAlert("Not allowed", "You can only promote an infrarank.");
				}
			});
			mainMenu.add(mItem);

			mItem = new MenuItem();
			mItem.setText("Demote Taxon");
			mItem.setIconStyle("icon-demote");
			mItem.addSelectionListener(new SelectionListener<MenuEvent>() {
				public void componentSelected(MenuEvent ce) {
					TaxonNode node = TaxonomyCache.impl.getCurrentNode();
					if (node == null || node.getLevel() != TaxonNode.SPECIES)
						WindowUtils.infoAlert("Not allowed", "You can only demote a species.");
					else
						popupChooser("Demote Taxon", "icon-demote", new TaxomaticDemotePanel(panelManager));
				}
			});
			mainMenu.add(mItem);

			mItem = new MenuItem();
			mItem.setIconStyle("icon-merge");
			mItem.setText("Merge Taxa");
			mItem.addSelectionListener(new SelectionListener<MenuEvent>() {
				public void componentSelected(MenuEvent ce) {
					TaxonNode node = TaxonomyCache.impl.getCurrentNode();
					if (node != null)
						popupChooser("Perform Merge", "icon-merge", new MergePanel());

				}
			});
			mainMenu.add(mItem);

			mItem = new MenuItem();
			mItem.setIconStyle("icon-merge-up");
			mItem.setText("Merge Up Subspecies");
			mItem.addSelectionListener(new SelectionListener<MenuEvent>() {

				@Override
				public void componentSelected(MenuEvent ce) {
					final TaxonNode node = TaxonomyCache.impl.getCurrentNode();
					if (node == null || node.getLevel() != TaxonNode.SPECIES) {
						WindowUtils.infoAlert("Not Allowed", "You can only merge subspecies into a species, please "
								+ "visit the species you wish to merge a subspecies into.");
					} else {
						TaxonomyCache.impl.getChildrenOfNode(node.getId() + "", new GenericCallback<List<TaxonNode>>() {

							public void onFailure(Throwable caught) {
								WindowUtils.infoAlert("Not Allowed", "There was an internal error while trying to "
										+ "fetch the children of " + node.getFullName());

							}

							public void onSuccess(List<TaxonNode> list) {
								boolean show = false;
								for (TaxonNode childNode : list) {
									if (childNode.getLevel() == TaxonNode.INFRARANK) {
										show = true;
										break;
									}
								}
								if (show) {
									popupChooser("Perform Merge Up Subspecies", "icon-merge-up", new MergeUpInfrarank());
								} else {
									WindowUtils
											.infoAlert(
													"Not Allowed",
													node.getFullName()
															+ " does not have any "
															+ "subspecies to promote.  You can only merge subspecies with their parent.");
								}

							}
						});
					}

				}
			});
			mainMenu.add(mItem);

			mItem = new MenuItem();
			mItem.setIconStyle("icon-split");
			mItem.setText("Split Taxon");
			mItem.addSelectionListener(new SelectionListener<MenuEvent>() {
				public void componentSelected(MenuEvent ce) {
					TaxonNode node = TaxonomyCache.impl.getCurrentNode();
					// TODO:
					// if( !node.isDeprecatedStatus() )
					popupChooser("Peform Partition", "icon-split", new SplitNodePanel());
					// else
					// WindowUtils.errorAlert("Error",
					// "Taxon selected for merging is not a valid taxon" +
					// " (i.e. status is not A or U).");

				}
			});
			mainMenu.add(mItem);

			mItem = new MenuItem();
			mItem.setIconStyle("icon-remove");
			mItem.setText("Remove Taxon");
			mItem.addSelectionListener(new SelectionListener<MenuEvent>() {
				public void componentSelected(MenuEvent ce) {
					final TaxonNode node = TaxonomyCache.impl.getCurrentNode();
					TaxonTreePopup.fetchChildren(node, new GenericCallback<List<TaxonListElement>>() {
						public void onFailure(Throwable caught) {
							ArrayList<String> assessments = node.getAssessments();
							String msg;
							if (assessments.size() > 0) {
								msg = "This taxa has assessments. These assessments will be moved to the trash on delete. Move"
										+ node.generateFullName() + " to the trash?";
							} else {
								msg = "Are you sure you want to send " + node.generateFullName() + " to the trash?";
							}

							WindowUtils.confirmAlert("Confirm Delete", msg, new WindowUtils.MessageBoxListener() {
								public void onNo() {
									// TODO Auto-generated method stub
									// close();

								}

								public void onYes() {
									// close();

									// remove assessments

									// remove node
									String url = "/taxomatic/";

									final String deleteUrl = url;
									if (TaxonomyCache.impl.getCurrentNode() != null) {
										final TaxonNode node = TaxonomyCache.impl.getCurrentNode();
										final NativeDocument doc = SimpleSISClient.getHttpBasicNativeDocument();

										doc.delete(deleteUrl + node.getId(), new GenericCallback<String>() {
											public void onFailure(Throwable arg0) {
												if( doc.getStatusText().equals("423") )
													WindowUtils.errorAlert("Taxomatic In Use", "Sorry, but another " +
															"taxomatic operation is currently running. Please try " +
															"again later!");
											}

											public void onSuccess(String arg0) {
												TaxonomyCache.impl.evict(node.getParentId() + "," + node.getId());
												TaxonomyCache.impl.fetchNode(node.getParentId(), true,
														new GenericCallback<TaxonNode>() {
															public void onFailure(Throwable caught) {
															};

															public void onSuccess(TaxonNode result) {
																AssessmentCache.impl.evictAssessments(node
																		.getAssessmentsCSV(),
																		AssessmentData.PUBLISHED_ASSESSMENT_STATUS);
																AssessmentCache.impl.evictAssessments(String
																		.valueOf(node.getId()),
																		AssessmentData.DRAFT_ASSESSMENT_STATUS);
																AssessmentCache.impl.evictAssessments(String
																		.valueOf(node.getId()),
																		AssessmentData.USER_ASSESSMENT_STATUS);

																ClientUIContainer.bodyContainer.tabManager.panelManager.taxonomicSummaryPanel
																		.update(String.valueOf(TaxonomyCache.impl
																				.getCurrentNode().getId()));
																panelManager.recentAssessmentsPanel.update();
															};
														});

											}
										});
									}
								}

							}, "Yes", "No");

						}

						public void onSuccess(List<TaxonListElement> result) {
							WindowUtils.infoAlert("You cannot remove this Taxa without first removing its children.");
							// ((List<ModelData>) result).size();

						}
					});
				}
			});
			mainMenu.add(mItem);

			// END TAXOMATIC FEATURES

			toolbar.add(taxomaticToolItem);
			toolbar.add(new SeparatorToolItem());

			mItem = new MenuItem();
			mItem.setIconStyle("icon-undo");
			mItem.setText("Undo Taxomatic Operation");
			mItem.addSelectionListener(new SelectionListener<MenuEvent>() {

				@Override
				public void componentSelected(MenuEvent ce) {
					final NativeDocument doc = SimpleSISClient.getHttpBasicNativeDocument();
					doc.getAsText("/taxomatic/undo", new GenericCallback<String>() {

						public void onFailure(Throwable caught) {
							WindowUtils.infoAlert("Unable to undo last operation", "SIS is unable to undo the last "
									+ "taxomatic operation "
									+ "because you are not the last user to perform a taxomatic "
									+ "change, or there has not been a taxomatic operation to undo.");

						}

						public void onSuccess(String result) {
							WindowUtils.confirmAlert("Undo Last Taxomatic Operation", doc.getText()
									+ "  Are you sure you want to undo this operation?",
									new WindowUtils.MessageBoxListener() {

										@Override
										public void onNo() {
											// TODO Auto-generated method stub

										}

										@Override
										public void onYes() {
											final NativeDocument postDoc = SimpleSISClient.getHttpBasicNativeDocument();
											postDoc.post("/taxomatic/undo", "", new GenericCallback<String>() {

												public void onFailure(Throwable caught) {
													WindowUtils
															.errorAlert("Unable to undo the last operation.  Please undo the operation manually.");

												}

												public void onSuccess(String result) {
													TaxonNode currentNode = TaxonomyCache.impl.getCurrentNode();
													TaxonomyCache.impl.clear();
													TaxonomyCache.impl.fetchNode(currentNode.getId() + "", true,
															new GenericCallback<TaxonNode>() {

																public void onFailure(Throwable caught) {

																	WindowUtils
																			.infoAlert("Success",
																					"Successfully undid the last taxomatic operation, but was unable to refresh the current taxon.");

																}

																public void onSuccess(TaxonNode result) {
																	ClientUIContainer.bodyContainer.refreshBody();
																	WindowUtils
																			.infoAlert("Success",
																					"Successfully undid the last taxomatic operation.");

																}
															});

												}
											});

										}
									});

						}
					});

				}
			});
		}
		mainMenu.add(mItem);

		mItem = new MenuItem();
		mItem.setText("Move assessments");
		mItem.addSelectionListener(new SelectionListener<MenuEvent>() {

			@Override
			public void componentSelected(MenuEvent ce) {
				TaxomaticAssessmentMover mover = new TaxomaticAssessmentMover(TaxonomyCache.impl.getCurrentNode());
				popupChooser("Move Assessments", "icon-document-move", mover);
			}
		});
		mItem.setIconStyle("icon-document-move");

		mainMenu.add(mItem);

		return toolbar;
	}

	private void popupChooser(String title, String iconStyle, LayoutContainer chooser) {
		final Window shell = WindowUtils.getWindow(true, false, title);
		chooser.sinkEvents(Events.Close.getEventCode());
		chooser.addListener(Events.Close, new Listener<BaseEvent>() {
			public void handleEvent(BaseEvent be) {
				shell.close();
			}
		});
		shell.setLayout(new FillLayout());
		shell.add(chooser);
		shell.show();
		shell.setSize(TaxonChooser.PANEL_WIDTH + 30, TaxonChooser.PANEL_HEIGHT + 50);
		shell.center();
	}

	private void popupChooserPromote(String title, String iconStyle, LayoutContainer chooser) {
		final Window shell = WindowUtils.getWindow(false, false, title);
		chooser.sinkEvents(Events.Close.getEventCode());
		chooser.addListener(Events.Close, new Listener() {
			public void handleEvent(BaseEvent be) {
				shell.close();
			}
		});
		shell.setLayout(new FillLayout());
		shell.add(chooser);
		shell.show();
		shell.setSize(500, 300);
		shell.center();
	}

	public void setAppropriateRights(TaxonNode node) {
		if (!AuthorizationCache.impl.hasRight(SimpleSISClient.currentUser, AuthorizableObject.WRITE, node)) {
			Info.display("Insufficient Rights", "Notice: You do not have "
					+ "sufficient permissions to edit this taxon.");
			taxonToolsItem.hide();
			taxomaticToolItem.hide();
		} else if (!AuthorizationCache.impl.hasRight(SimpleSISClient.currentUser, AuthorizableObject.USE_FEATURE, AuthorizableFeature.TAXOMATIC_FEATURE)) {
			taxonToolsItem.show();
			taxomaticToolItem.hide();
		} else {
			taxonToolsItem.show();
			taxomaticToolItem.show();
		}

		layout();
	}

}
