/*
 * 
 * This file is part of Http-Server
 * Copyright (C) 2013  Jatinder
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this library; If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package singh.jatinder.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

/**
 * @author Jatinder
 *
 * Utility to make quick pages..
 *
 */
public class ResponseUtils {

	//private static final Logger LOG = LoggerFactory.getLogger(ResponseUtils.class);
	public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
    public static final int HTTP_CACHE_SECONDS = 60;
	
	/** Precomputed 404 response. */
	public static final FullHttpResponse PAGE_NOT_FOUND = new DefaultFullHttpResponse(HttpVersion.HTTP_1_0, HttpResponseStatus.NOT_FOUND, makePage(null, "Page Not Found", "Error 404",
			new StringBuilder("<blockquote> <h1>Page Not Found</h1> The requested URL was not found on this server.</blockquote>")));
	
	
	  // -------------------------------------------- //
	  // Boilerplate (from Google) //
	  // -------------------------------------------- //

	  private static final String PAGE_HEADER_START =
	    "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">"
	    + "<html><head>"
	    + "<meta http-equiv=content-type content=\"text/html;charset=utf-8\">"
	    + "<title>";

	  private static final String PAGE_HEADER_MID =
	    "</title>\n"
	    + "<style><!--\n"
	    + "body{font-family:arial,sans-serif;margin-left:2em}"
	    + "A.l:link{color:#6f6f6f}"
	    + "A.u:link{color:green}"
	    + ".subg{background-color:#e2f4f7}"
	    + ".fwf{font-family:monospace;white-space:pre-wrap}"
	    + "//--></style>";

	  private static final String PAGE_HEADER_END_BODY_START =
	    "</head>\n"
	    + "<body text=#000000 bgcolor=#ffffff>"
	    + "<table border=0 cellpadding=2 cellspacing=0 width=100%>"
	    + "<tr><td rowspan=3 width=1% nowrap><b>"
	    + "<font color=#E30F00 size=10>Http-Server</font>"
	    + "&nbsp;&nbsp;</b><td>&nbsp;</td></tr>"
	    + "<tr><td class=subg><font color=#507e9b><b>";

	  private static final String PAGE_BODY_MID =
	    "</b></td></tr>"
	    + "<tr><td>&nbsp;</td></tr></table><div id=\"workspace\"> </div>";

	  private static final String PAGE_FOOTER =
	    "<table width=100% cellpadding=0 cellspacing=0>"
	    + "<tr><td class=subg><font color=#507e9b size=2>© Simple Http-Server<img alt=\"\" width=1 height=3></td></tr>"
	    + "</table></body></html>";

	  private static final int BOILERPLATE_LENGTH =
	    PAGE_HEADER_START.length()
	    + PAGE_HEADER_MID.length()
	    + PAGE_HEADER_END_BODY_START.length()
	    + PAGE_BODY_MID.length()
	    + PAGE_FOOTER.length();

	  public static ByteBuf getChannelBuffer (StringBuilder buffer) {
		  return Unpooled.wrappedBuffer(buffer.toString().getBytes());
	  }
	   
	   /**
		* Easy way to generate a small, simple HTML page.
		* @param htmlheader Text to insert in the {@code head} tag.
		* Ignored if {@code null}.
		* @param title What should be in the {@code title} tag of the page.
		* @param subtitle Small sentence to use next to the Controller logo.
		* @param body The body of the page (excluding the {@code body} tag).
		* @return A full HTML page.
		*/
	  public static ByteBuf makePage(final String htmlheader, final String title,
		                                        final String subtitle, final StringBuilder body) {
		    final StringBuilder buf = new StringBuilder(
		      BOILERPLATE_LENGTH + (htmlheader == null ? 0 : htmlheader.length())
		      + title.length() + subtitle.length() + body.length());
		    buf.append(PAGE_HEADER_START).append(title).append(PAGE_HEADER_MID);
		    if (htmlheader != null) {
		      buf.append(htmlheader);
		    }
		    buf.append(PAGE_HEADER_END_BODY_START).append(subtitle)
		      .append(PAGE_BODY_MID).append(body).append(PAGE_FOOTER);
		    return getChannelBuffer(buf);
		  }
	  
	  public static FullHttpResponse makeSimpleHtmlResponse(ChannelHandlerContext context, HttpVersion version, HttpResponseStatus status, String message) {
		  ByteBuf buffer = context.alloc().buffer(message.length());
		  buffer.writeBytes(message.getBytes());
		  FullHttpResponse response = new DefaultFullHttpResponse(version, status, buffer);
		  response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/html");
		  return response;
	  }
}
