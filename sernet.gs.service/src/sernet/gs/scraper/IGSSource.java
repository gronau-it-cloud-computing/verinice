/*******************************************************************************
 * Copyright (c) 2009 Alexander Koderman <ak@sernet.de>.
 * This program is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License 
 * as published by the Free Software Foundation, either version 3 
 * of the License, or (at your option) any later version.
 *     This program is distributed in the hope that it will be useful,    
 * but WITHOUT ANY WARRANTY; without even the implied warranty 
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU General Public License for more details.
 *     You should have received a copy of the GNU General Public 
 * License along with this program. 
 * If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contributors:
 *     Alexander Koderman <ak@sernet.de> - initial API and implementation
 ******************************************************************************/
package sernet.gs.scraper;

import java.io.InputStream;

import org.w3c.dom.Node;

import sernet.gs.service.GSServiceException;

/**
 * Interface for all types of data sources
 * containing BSI GS-Catalogue information. 
 * 
 * @author akoderman@sernet.de
 *
 */
public interface IGSSource {
	
	public Node parseBausteinDocument(String path) throws GSServiceException;
	public Node parseMassnahmenDocument(String path) throws GSServiceException;
	public InputStream getBausteinAsStream(String baustein) throws GSServiceException;
	public InputStream getMassnahmeAsStream(String massnahme) throws GSServiceException;
	public InputStream getGefaehrdungAsStream(String gefaehrdung) throws GSServiceException;
}
