/* Rdesktop.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 13 $
 * Author: $Author: miha_vitorovic $
 * Date: $Date: 2007-05-11 05:14:45 -0700 (Fri, 11 May 2007) $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Main class, launches session
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA
 * 
 * (See gpl.txt for details of the GNU General Public License.)
 * 
 */
package net.propero.rdp;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import net.propero.rdp.keymapping.KeyCode_FileBased;
import net.propero.rdp.rdp5.Rdp5;
import net.propero.rdp.rdp5.VChannels;
import net.propero.rdp.rdp5.cliprdr.ClipChannel;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class Rdesktop {

	/**
	 * Translate a disconnect code into a textual description of the reason for
	 * the disconnect
	 * 
	 * @param reason
	 *            Integer disconnect code received from server
	 * @return Text description of the reason for disconnection
	 */
	static String textDisconnectReason(int reason) {
		String text;

		switch (reason) {
		case exDiscReasonNoInfo:
			text = "No information available";
			break;

		case exDiscReasonAPIInitiatedDisconnect:
			text = "Server initiated disconnect";
			break;

		case exDiscReasonAPIInitiatedLogoff:
			text = "Server initiated logoff";
			break;

		case exDiscReasonServerIdleTimeout:
			text = "Server idle timeout reached";
			break;

		case exDiscReasonServerLogonTimeout:
			text = "Server logon timeout reached";
			break;

		case exDiscReasonReplacedByOtherConnection:
			text = "Another user connected to the session";
			break;

		case exDiscReasonOutOfMemory:
			text = "The server is out of memory";
			break;

		case exDiscReasonServerDeniedConnection:
			text = "The server denied the connection";
			break;

		case exDiscReasonServerDeniedConnectionFips:
			text = "The server denied the connection for security reason";
			break;

		case exDiscReasonLicenseInternal:
			text = "Internal licensing error";
			break;

		case exDiscReasonLicenseNoLicenseServer:
			text = "No license server available";
			break;

		case exDiscReasonLicenseNoLicense:
			text = "No valid license available";
			break;

		case exDiscReasonLicenseErrClientMsg:
			text = "Invalid licensing message";
			break;

		case exDiscReasonLicenseHwidDoesntMatchLicense:
			text = "Hardware id doesn't match software license";
			break;

		case exDiscReasonLicenseErrClientLicense:
			text = "Client license error";
			break;

		case exDiscReasonLicenseCantFinishProtocol:
			text = "Network error during licensing protocol";
			break;

		case exDiscReasonLicenseClientEndedProtocol:
			text = "Licensing protocol was not completed";
			break;

		case exDiscReasonLicenseErrClientEncryption:
			text = "Incorrect client license enryption";
			break;

		case exDiscReasonLicenseCantUpgradeLicense:
			text = "Can't upgrade license";
			break;

		case exDiscReasonLicenseNoRemoteConnections:
			text = "The server is not licensed to accept remote connections";
			break;

		default:
			if (reason > 0x1000 && reason < 0x7fff) {
				text = "Internal protocol error";
			} else {
				text = "Unknown reason";
			}
		}
		return text;
	}

	/* RDP5 disconnect PDU */
	public static final int exDiscReasonNoInfo = 0x0000;

	public static final int exDiscReasonAPIInitiatedDisconnect = 0x0001;

	public static final int exDiscReasonAPIInitiatedLogoff = 0x0002;

	public static final int exDiscReasonServerIdleTimeout = 0x0003;

	public static final int exDiscReasonServerLogonTimeout = 0x0004;

	public static final int exDiscReasonReplacedByOtherConnection = 0x0005;

	public static final int exDiscReasonOutOfMemory = 0x0006;

	public static final int exDiscReasonServerDeniedConnection = 0x0007;

	public static final int exDiscReasonServerDeniedConnectionFips = 0x0008;

	public static final int exDiscReasonLicenseInternal = 0x0100;

	public static final int exDiscReasonLicenseNoLicenseServer = 0x0101;

	public static final int exDiscReasonLicenseNoLicense = 0x0102;

	public static final int exDiscReasonLicenseErrClientMsg = 0x0103;

	public static final int exDiscReasonLicenseHwidDoesntMatchLicense = 0x0104;

	public static final int exDiscReasonLicenseErrClientLicense = 0x0105;

	public static final int exDiscReasonLicenseCantFinishProtocol = 0x0106;

	public static final int exDiscReasonLicenseClientEndedProtocol = 0x0107;

	public static final int exDiscReasonLicenseErrClientEncryption = 0x0108;

	public static final int exDiscReasonLicenseCantUpgradeLicense = 0x0109;

	public static final int exDiscReasonLicenseNoRemoteConnections = 0x010a;

	static Logger logger = Logger.getLogger("net.propero.rdp");

	static boolean keep_running;

	static boolean loggedon;

	static boolean readytosend;

	static final String keyMapPath = "keymaps/";

	static String mapFile = "en-gb";

	static String keyMapLocation = "";

	/**
	 * 
	 * @param args
	 * @throws OrderException
	 * @throws RdesktopException
	 */
	public static void main(String[] args) throws OrderException,
			RdesktopException {

		BasicConfigurator.configure();

		// Ensure that static variables are properly initialised
		keep_running = true;
		loggedon = false;
		readytosend = false;
		mapFile = "en-gb";
		keyMapLocation = "";

		logger.setLevel(Level.DEBUG);

		String server = "swwwooby";
		int logonflags = Rdp.RDP_LOGON_NORMAL;
		boolean fFullscreen = false;
		boolean fFullscreenKdeHack = false;

		if (fFullscreen) {
			Dimension screen_size = Toolkit.getDefaultToolkit().getScreenSize();
			// ensure width a multiple of 4
			Options.width = screen_size.width & ~3;
			Options.height = screen_size.height;
			Options.fullscreen = true;
			if (fFullscreenKdeHack) {
				Options.height -= 46;
			}
		} else {
			Options.width = 800;
			Options.height = 600;
		}

		logger.info("properJavaRDP version " + Version.version);

		String java = System.getProperty("java.specification.version");
		logger.info("Java version is " + java);

		String os = System.getProperty("os.name");
		String osvers = System.getProperty("os.version");

		logger.info("Operating System is " + os + " version " + osvers);

		if (os.startsWith("Linux"))
			Constants.OS = Constants.LINUX;
		else if (os.startsWith("Windows")) {
			Options.built_in_licence = true;
			Constants.OS = Constants.WINDOWS;
		} else if (os.startsWith("Mac"))
			Constants.OS = Constants.MAC;

		// Now do the startup...

		VChannels channels = new VChannels();
		ClipChannel clipChannel = new ClipChannel();

		// Initialise all RDP5 channels
		if (Options.use_rdp5) {
			// TODO: implement all relevant channels
			if (Options.map_clipboard) {
				channels.register(clipChannel);
			}
		}

		Rdp5 RdpLayer = null;
		Common.rdp = RdpLayer;
		RdesktopFrame window = new RdesktopFrame_Localised();
		window.setClip(clipChannel);

		// Configure a keyboard layout
		KeyCode_FileBased keyMap = null;
		try {
			// logger.info("looking for: " + "/" + keyMapPath + mapFile);
			InputStream istr = Rdesktop.class.getResourceAsStream("/"
					+ keyMapPath + mapFile);
			// logger.info("istr = " + istr);
			if (istr == null) {
				logger.debug("Loading keymap from filename");
				keyMap = new KeyCode_FileBased_Localised(keyMapPath + mapFile);
			} else {
				logger.debug("Loading keymap from InputStream");
				keyMap = new KeyCode_FileBased_Localised(istr);
			}
			if (istr != null)
				istr.close();
			Options.keylayout = keyMap.getMapCode();
		} catch (Exception kmEx) {
			String[] msg = { (kmEx.getClass() + ": " + kmEx.getMessage()) };
			window.showErrorDialog(msg);
			kmEx.printStackTrace();
			Rdesktop.exit(0, null, null, true);
		}
		logger.debug("Registering keyboard...");
		if (keyMap != null)
			window.registerKeyboard(keyMap);

		boolean[] deactivated = new boolean[1];
		int[] ext_disc_reason = new int[1];

		logger.debug("keep_running = " + keep_running);
		while (keep_running) {
			logger.debug("Initialising RDP layer...");
			RdpLayer = new Rdp5(channels);
			Common.rdp = RdpLayer;
			logger.debug("Registering drawing surface...");
			RdpLayer.registerDrawingSurface(window);
			logger.debug("Registering comms layer...");
			window.registerCommLayer(RdpLayer);
			loggedon = false;
			readytosend = false;
			logger
					.info("Connecting to " + server + ":" + Options.port
							+ " ...");

			if (server.equalsIgnoreCase("localhost"))
				server = "127.0.0.1";

			if (RdpLayer != null) {
				// Attempt to connect to server on port Options.port
				try {
					RdpLayer.connect(Options.username, InetAddress
							.getByName(server), logonflags, Options.domain,
							Options.password, Options.command,
							Options.directory);
					if (keep_running) {

						//
						// By setting encryption to False here, we have an
						// encrypted login packet but unencrypted transfer of
						// other packets
						//
						if (!Options.packet_encryption)
							Options.encryption = false;

						logger.info("Connection successful");
						// now show window after licence negotiation
						RdpLayer.mainLoop(deactivated, ext_disc_reason);

						if (deactivated[0]) {
							// clean disconnect
							Rdesktop.exit(0, RdpLayer, window, true);
						} else {
							if (ext_disc_reason[0] == exDiscReasonAPIInitiatedDisconnect
									|| ext_disc_reason[0] == exDiscReasonAPIInitiatedLogoff) {
								// not so clean disconnect, but nothing to worry
								// about
								Rdesktop.exit(0, RdpLayer, window, true);
							}
							if (ext_disc_reason[0] >= 2) {
								String reason = textDisconnectReason(ext_disc_reason[0]);
								String msg[] = { "Connection terminated",
										reason };
								window.showErrorDialog(msg);
								logger.warn("Connection terminated: " + reason);
								Rdesktop.exit(0, RdpLayer, window, true);
							}
						}
						keep_running = false; // exited main loop
						if (!readytosend) {
							// maybe the licence server was having a comms
							// problem; retry?
							String msg1 = "The terminal server disconnected before licence negotiation completed.";
							String msg2 = "Possible cause: terminal server could not issue a licence.";
							String[] msg = { msg1, msg2 };
							logger.warn(msg1);
							logger.warn(msg2);
							window.showErrorDialog(msg);
						}
					}

				} catch (ConnectionException e) {
					String msg[] = { "Connection Exception", e.getMessage() };
					window.showErrorDialog(msg);
					Rdesktop.exit(0, RdpLayer, window, true);
				} catch (UnknownHostException e) {
					error(e, RdpLayer, window, true);
				} catch (SocketException s) {
					if (RdpLayer.isConnected()) {
						logger.fatal(s.getClass().getName() + " "
								+ s.getMessage());
						// s.printStackTrace();
						error(s, RdpLayer, window, true);
						Rdesktop.exit(0, RdpLayer, window, true);
					}
				} catch (RdesktopException e) {
					String msg1 = e.getClass().getName();
					String msg2 = e.getMessage();
					logger.fatal(msg1 + ": " + msg2);

					e.printStackTrace(System.err);

					if (!readytosend) {
						// maybe the licence server was having a comms problem,
						// retry?
						String msg[] = {
								"The terminal server reset connection before licence negotiation completed.",
								"Possible cause: terminal server could not connect to licence server.",
								"Retry?" };
						boolean retry = window.showYesNoErrorDialog(msg);
						if (!retry) {
							logger.info("Selected not to retry.");
							Rdesktop.exit(0, RdpLayer, window, true);
						} else {
							if (RdpLayer != null && RdpLayer.isConnected()) {
								logger.info("Disconnecting ...");
								RdpLayer.disconnect();
								logger.info("Disconnected");
							}
							logger.info("Retrying connection...");
							keep_running = true; // retry
							continue;
						}
					} else {
						String msg[] = { e.getMessage() };
						window.showErrorDialog(msg);
						Rdesktop.exit(0, RdpLayer, window, true);
					}
				} catch (Exception e) {
					logger.warn(e.getClass().getName() + " " + e.getMessage());
					e.printStackTrace();
					error(e, RdpLayer, window, true);
				}
			} else {
				logger
						.fatal("The communications layer could not be initiated!");
			}
		}
		Rdesktop.exit(0, RdpLayer, window, true);
	}

	/**
	 * Disconnects from the server connected to through rdp and destroys the
	 * RdesktopFrame window.
	 * <p>
	 * Exits the application iff sysexit == true, providing return value n to
	 * the operating system.
	 * 
	 * @param n
	 * @param rdp
	 * @param window
	 * @param sysexit
	 */
	public static void exit(int n, Rdp rdp, RdesktopFrame window,
			boolean sysexit) {
		keep_running = false;

		if (rdp != null && rdp.isConnected()) {
			logger.info("Disconnecting ...");
			rdp.disconnect();
			logger.info("Disconnected");
		}
		if (window != null) {
			window.setVisible(false);
			window.dispose();
		}

		System.gc();

		if (sysexit && Constants.SystemExit) {
			if (!Common.underApplet)
				System.exit(n);
		}
	}

	/**
	 * Displays an error dialog via the RdesktopFrame window containing the
	 * customised message emsg, and reports this through the logging system.
	 * <p>
	 * The application then exits iff sysexit == true
	 * 
	 * @param emsg
	 * @param RdpLayer
	 * @param window
	 * @param sysexit
	 */
	public static void customError(String emsg, Rdp RdpLayer,
			RdesktopFrame window, boolean sysexit) {
		logger.fatal(emsg);
		String[] msg = { emsg };
		window.showErrorDialog(msg);
		Rdesktop.exit(0, RdpLayer, window, true);
	}

	/**
	 * Displays details of the Exception e in an error dialog via the
	 * RdesktopFrame window and reports this through the logger, then prints a
	 * stack trace.
	 * <p>
	 * The application then exits iff sysexit == true
	 * 
	 * @param e
	 * @param RdpLayer
	 * @param window
	 * @param sysexit
	 */
	public static void error(Exception e, Rdp RdpLayer, RdesktopFrame window,
			boolean sysexit) {
		try {

			String msg1 = e.getClass().getName();
			String msg2 = e.getMessage();

			logger.fatal(msg1 + ": " + msg2);

			String[] msg = { msg1, msg2 };
			window.showErrorDialog(msg);

			// e.printStackTrace(System.err);
		} catch (Exception ex) {
			logger.warn("Exception in Rdesktop.error: "
					+ ex.getClass().getName() + ": " + ex.getMessage());
		}

		Rdesktop.exit(0, RdpLayer, window, sysexit);
	}
}
