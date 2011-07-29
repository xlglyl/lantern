package org.lantern;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.lang.SystemUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for handling all system tray interactions.
 */
public class SystemTrayImpl implements SystemTray {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Display display;
    private final Shell shell;
    private TrayItem trayItem;
    private MenuItem updateItem;
    private Menu menu;
    private Map<String, String> updateData;

    /**
     * Creates a new system tray handler class.
     * 
     * @param display The SWT display. 
     */
    public SystemTrayImpl(final Display display) {
        this.display = display;
        this.shell = new Shell(display);
    }

    @Override
    public void createTray() {
        display.asyncExec (new Runnable () {
            @Override
            public void run () {
                createTrayInternal();
            }
        });
    }
    
    private void createTrayInternal() {
        final Tray tray = display.getSystemTray ();
        if (tray == null) {
            System.out.println ("The system tray is not available");
        } else {
            this.trayItem = new TrayItem (tray, SWT.NONE);
            this.trayItem.setToolTipText("Lantern");
            this.trayItem.addListener (SWT.Show, new Listener () {
                @Override
                public void handleEvent (final Event event) {
                    System.out.println("show");
                }
            });
            this.trayItem.addListener (SWT.Hide, new Listener () {
                @Override
                public void handleEvent (final Event event) {
                    System.out.println("hide");
                }
            });

            this.menu = new Menu (shell, SWT.POP_UP);
            final MenuItem configItem = new MenuItem(menu, SWT.PUSH);
            configItem.setText("Configure");
            configItem.addListener (SWT.Selection, new Listener () {
                @Override
                public void handleEvent (final Event event) {
                    System.out.println("Got config call");
                    /*
                    FileDialog fd = new FileDialog(shell, SWT.OPEN);
                    fd.setText("Open");
                    //fd.setFilterPath("C:/");
                    //String[] filterExt = { "*.txt", "*.doc", ".rtf", "*.*" };
                    //fd.setFilterExtensions(filterExt);
                    String selected = fd.open();
                    shell.forceActive();
                    System.out.println(selected);
                    */
                    
                    final LanternBrowser browser = new LanternBrowser(true);
                    browser.install();
                }
            });
            
            
            new MenuItem(menu, SWT.SEPARATOR);
            
            final MenuItem quitItem = new MenuItem(menu, SWT.PUSH);
            quitItem.setText("Quit");
            
            quitItem.addListener (SWT.Selection, new Listener () {
                @Override
                public void handleEvent (final Event event) {
                    System.out.println("Got exit call");
                    display.dispose();
                    System.exit(0);
                }
            });
            //menu.setDefaultItem(quitItem);

            trayItem.addListener (SWT.MenuDetect, new Listener () {
                @Override
                public void handleEvent (final Event event) {
                    System.out.println("Setting menu visible");
                    menu.setVisible (true);
                }
            });
            
            final String imageName;
            if (SystemUtils.IS_OS_MAC_OSX) {
                imageName = "16off.png";
            } else {
                imageName = "16on.png";
            }
            final Image image = newImage(imageName, 16, 16);
            setImage(image);
        }
    }

    private void setImage(final Image image) {
        display.asyncExec (new Runnable () {
            @Override
            public void run () {
                trayItem.setImage (image);
            }
        });
    }

    private Image newImage(final String name, int width, int height) {
        final File iconFile;
        final File iconCandidate1 = new File("install/common/"+name);
        if (iconCandidate1.isFile()) {
            iconFile = iconCandidate1;
        } else {
            iconFile = new File(name);
        }
        if (!iconFile.isFile()) {
            log.error("Still no icon file at: " + iconFile);
        }
        InputStream is = null;
        try {
            is = new FileInputStream(iconFile);
            return new Image (display, is);
        } catch (final FileNotFoundException e) {
            log.error("Could not find icon file: "+iconFile, e);
        } 
        return new Image (display, width, height);
    }
    
    @Override
    public void activate() {
        log.info("Activating Lantern icon");
        if (!SystemUtils.IS_OS_MAC_OSX) {
            log.info("Ignoring activation since we're not on OSX...");
            return;
        }
        final Image image = newImage("16on.png", 16, 16);
        setImage(image);
    }
    

    @Override
    public void addUpdate(final Map<String, String> data) {
        log.info("Adding update data: {}", data);
        this.updateData = data;
        display.asyncExec (new Runnable () {
            @Override
            public void run () {
                if (updateItem == null) {
                    updateItem = new MenuItem(menu, SWT.PUSH, 0);
                    updateItem.addListener (SWT.Selection, new Listener () {
                        @Override
                        public void handleEvent (final Event event) {
                            log.info("Got update call");
                            NativeUtils.openUri(updateData.get(
                                LanternConstants.UPDATE_URL_KEY));
                        }
                    });
                }
                updateItem.setText("Update to Lantern "+
                    data.get(LanternConstants.UPDATE_VERSION_KEY));
            }
        });
    }
}
