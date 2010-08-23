// Blog.java
// -----------------------
// part of YACY
// (C) by Michael Peter Christen; mc@yacy.net
// first published on http://www.anomic.de
// Frankfurt, Germany, 2004
//
// This File is contributed by Jan Sandbrink
// Contains contributions from Marc Nause [MN]
//
//$LastChangedDate$
//$LastChangedRevision$
//$LastChangedBy$
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

// You must compile this file with
// javac -classpath .:../classes Blog.java
// if the shell's current path is HTROOT

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

import net.yacy.cora.protocol.HeaderFramework;
import net.yacy.cora.protocol.RequestHeader;

import de.anomic.data.blogBoard;
import de.anomic.data.userDB;
import de.anomic.search.Switchboard;
import de.anomic.server.serverObjects;
import de.anomic.server.serverSwitch;
import de.anomic.yacy.yacyNewsPool;
import java.util.List;
import java.util.Map;

public class Blog {

    private static final String DEFAULT_PAGE = "blog_default";

        private static SimpleDateFormat SimpleFormatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US);
        // TODO: make userdefined date/time-strings (localisation)

    public static String dateString(final Date date) {
        return SimpleFormatter.format(date);
    }

    public static serverObjects respond(final RequestHeader header, final serverObjects post, final serverSwitch env) {
        final Switchboard sb = (Switchboard) env;
        final serverObjects prop = new serverObjects();
        blogBoard.BlogEntry page = null;

        boolean hasRights = sb.verifyAuthentication(header, true);
        
        //final int display = (hasRights || post == null) ? 1 : post.getInt("display", 0);
        //prop.put("display", display);   
        prop.put("display", 1); // Fixed to 1

        
        final boolean xml = (header.get(HeaderFramework.CONNECTION_PROP_PATH)).endsWith(".xml");
        final String address = sb.peers.mySeed().getPublicAddress();

        if(hasRights) {
            prop.put("mode_admin", "1");
        } else {
            prop.put("mode_admin", "0");
        }

        if (post == null) {
            prop.putHTML("peername", sb.peers.mySeed().getName());
            prop.put("address", address);
            return putBlogDefault(prop, sb, address, 0, 10, hasRights, xml);
        }

        final int start = post.getInt("start",0); //indicates from where entries should be shown
        final int num   = post.getInt("num",10);  //indicates how many entries should be shown

        if(!hasRights){
            final userDB.Entry userentry = sb.userDB.proxyAuth(header.get(RequestHeader.AUTHORIZATION, "xxxxxx"));
            if(userentry != null && userentry.hasRight(userDB.Entry.BLOG_RIGHT)){
                hasRights=true;
            } else if(post.containsKey("login")) {
                //opens login window if login link is clicked - contrib [MN]
                prop.put("AUTHENTICATE","admin log-in");
            }
        }

        String pagename = post.get("page", DEFAULT_PAGE);
        final String ip = header.get(HeaderFramework.CONNECTION_PROP_CLIENTIP, "127.0.0.1");

        String StrAuthor = post.get("author", "");

        if (StrAuthor.equals("anonymous")) {
            StrAuthor = sb.blogDB.guessAuthor(ip);

            if (StrAuthor == null || StrAuthor.length() == 0) {
                if (sb.peers.mySeed() == null) {
                    StrAuthor = "anonymous";
                } else {
                    StrAuthor = sb.peers.mySeed().get("Name", "anonymous");
                }
            }
        }

        byte[] author;
        try {
            author = StrAuthor.getBytes("UTF-8");
        } catch (final UnsupportedEncodingException e) {
            author = StrAuthor.getBytes();
        }

        if(hasRights && post.containsKey("delete") && post.get("delete").equals("sure")) {
            page = sb.blogDB.readBlogEntry(pagename);
            for (final String comment : page.getComments()) {
                sb.blogCommentDB.delete(comment);
            }
            sb.blogDB.deleteBlogEntry(pagename);
            pagename = DEFAULT_PAGE;
        }

        if (post.containsKey("discard")) {
            pagename = DEFAULT_PAGE;
        }

        if (post.containsKey("submit") && (hasRights)) {
            // store a new/edited blog-entry
            byte[] content;
            try {
                content = post.get("content", "").getBytes("UTF-8");
            } catch (final UnsupportedEncodingException e) {
                content = post.get("content", "").getBytes();
            }

            Date date = null;
            List<String> comments = null;

            //set name for new entry or date for old entry
            if(pagename.equals(DEFAULT_PAGE)) {
                pagename = String.valueOf(System.currentTimeMillis());
            } else {
                page = sb.blogDB.readBlogEntry(pagename);
                comments = page.getComments();
                date = page.getDate();
            }
            final String commentMode = post.get("commentMode", "2");
            final String StrSubject = post.get("subject", "");
            byte[] subject;
            try {
                subject = StrSubject.getBytes("UTF-8");
            } catch (final UnsupportedEncodingException e) {
                subject = StrSubject.getBytes();
            }

            sb.blogDB.writeBlogEntry(sb.blogDB.newEntry(pagename, subject, author, ip, date, content, comments, commentMode));

            // create a news message
            final Map<String, String> map = new HashMap<String, String>();
            map.put("page", pagename);
            map.put("subject", StrSubject.replace(',', ' '));
            map.put("author", StrAuthor.replace(',', ' '));
            sb.peers.newsPool.publishMyNews(sb.peers.mySeed(), yacyNewsPool.CATEGORY_BLOG_ADD, map);
        }

        page = sb.blogDB.readBlogEntry(pagename); //maybe "if(page == null)"

        if (post.containsKey("edit")) {
            //edit an entry
            if(hasRights) {
                try {
                    prop.put("mode", "1"); //edit
                    prop.put("mode_commentMode", page.getCommentMode());
                    prop.putHTML("mode_author", new String(page.getAuthor(),"UTF-8"));
                    prop.put("mode_pageid", page.getKey());
                    prop.putHTML("mode_subject", new String(page.getSubject(), "UTF-8"));
                    prop.put("mode_page-code", new String(page.getPage(), "UTF-8"));
                } catch (final UnsupportedEncodingException e) {}
            }
            else {
                prop.put("mode", "3"); //access denied (no rights)
            }
        } else if(post.containsKey("preview")) {
            //preview the page
            if(hasRights) {
                prop.put("mode", "2");//preview
                prop.put("mode_commentMode", post.getInt("commentMode", 2));
                prop.putHTML("mode_pageid", pagename);
                try {
                    prop.putHTML("mode_author", new String(author, "UTF-8"));
                } catch (final UnsupportedEncodingException e) {
                    prop.putHTML("mode_author", new String(author));
                }
                prop.putHTML("mode_subject", post.get("subject",""));
                prop.put("mode_date", dateString(new Date()));
                prop.putWiki("mode_page", post.get("content", ""));
                prop.putHTML("mode_page-code", post.get("content", ""));
            }
            else {
                prop.put("mode", "3"); //access denied (no rights)
            }
        }
        else if(post.get("delete", "").equals("try")) {
            if(hasRights) {
                prop.put("mode", "4");
                prop.putHTML("mode_pageid", pagename);
                try {
                    prop.putHTML("mode_author",new String(page.getAuthor(), "UTF-8"));
                } catch (final UnsupportedEncodingException e) {
                    prop.putHTML("mode_author",new String(page.getAuthor()));
                }
                try {
                    prop.putHTML("mode_subject",new String(page.getSubject(),"UTF-8"));
                } catch (final UnsupportedEncodingException e) {
                    prop.putHTML("mode_subject",new String(page.getSubject()));
                }
            }
            else prop.put("mode", "3"); //access denied (no rights)
        }
        else if (post.containsKey("import")) {
            prop.put("mode", "5");
            prop.put("mode_state", "0");
        }
        else if (post.containsKey("xmlfile")) {
            prop.put("mode", "5");
            if(sb.blogDB.importXML(post.get("xmlfile$file"))) {
                prop.put("mode_state", "1");
            }
            else {
                prop.put("mode_state", "2");
            }
        }
        else {
            // show blog-entry/entries
            prop.put("mode", "0"); //viewing
            if(pagename.equals(DEFAULT_PAGE)) {
                // XXX: where are "peername" and "address" used in the template?
                // XXX: "clientname" is already set to the peername, no need for a new setting
                prop.putHTML("peername", sb.peers.mySeed().getName());
                prop.put("address", address);
                //index all entries
                putBlogDefault(prop, sb, address, start, num, hasRights, xml);
            }
            else {
                //only show 1 entry
                prop.put("mode_entries", "1");
                putBlogEntry(prop, page, address, 0, hasRights, xml);
            }
        }

        // return rewrite properties
        return prop;
    }

    private static serverObjects putBlogDefault(
            final serverObjects prop,
            final Switchboard switchboard,
            final String address,
            int start,
            int num,
            final boolean hasRights,
            final boolean xml) 
    {
            final Iterator<String> i = switchboard.blogDB.getBlogIterator(false);
            String pageid;
            int count = 0;                        //counts how many entries are shown to the user
            if(xml) num = 0;
            final int nextstart = start+num;      //indicates the starting offset for next results
            int prevstart = start-num;            //indicates the starting offset for previous results
            boolean prev = false;                 //indicates if there were previous comments to the ones that are dispalyed
            if (start > 0) prev = true;
            while(i.hasNext() && (num == 0 || num > count)) {
                pageid = i.next();
                if(0 < start--) continue;
                putBlogEntry(
                        prop,
                        switchboard.blogDB.readBlogEntry(pageid),
                        address,
                        count++,
                        hasRights,
                        xml);
            }
            prop.put("mode_entries", count);

            if(i.hasNext()) {
                prop.put("mode_moreentries", "1"); //more entries are availible
                prop.put("mode_moreentries_start", nextstart);
                prop.put("mode_moreentries_num", num);
            } else {
                prop.put("moreentries", "0");
            }
            
            if(prev) {
                prop.put("mode_preventries", "1");
                if (prevstart < 0) prevstart = 0;
                prop.put("mode_preventries_start", prevstart);
                prop.put("mode_preventries_num", num);
            } else prop.put("mode_preventries", "0");
            
            
        return prop;
    }

    private static serverObjects putBlogEntry(
            final serverObjects prop,
            final blogBoard.BlogEntry entry,
            final String address,
            final int number,
            final boolean hasRights,
            final boolean xml) 
    {
        // subject
        try {
            prop.putHTML("mode_entries_" + number + "_subject", new String(entry.getSubject(),"UTF-8"));
        } catch (final UnsupportedEncodingException e) {
            prop.putHTML("mode_entries_" + number + "_subject", new String(entry.getSubject()));
        }

        // author
        try {
            prop.putHTML("mode_entries_" + number + "_author", new String(entry.getAuthor(),"UTF-8"));
        } catch (final UnsupportedEncodingException e) {
            prop.putHTML("mode_entries_" + number + "_author", new String(entry.getAuthor()));
        }

        // comments
        if(entry.getCommentMode() == 0) {
            prop.put("mode_entries_" + number + "_commentsactive", "0");
        } else {
            prop.put("mode_entries_" + number + "_commentsactive", "1");
            prop.put("mode_entries_" + number + "_commentsactive_pageid", entry.getKey());
            prop.put("mode_entries_" + number + "_commentsactive_address", address);
            prop.put("mode_entries_" + number + "_commentsactive_comments", entry.getCommentsSize());
        }

        prop.put("mode_entries_" + number + "_date", dateString(entry.getDate()));
        prop.put("mode_entries_" + number + "_rfc822date", HeaderFramework.formatRFC1123(entry.getDate()));
        prop.put("mode_entries_" + number + "_pageid", entry.getKey());
        prop.put("mode_entries_" + number + "_address", address);
        prop.put("mode_entries_" + number + "_ip", entry.getIp());

        if(xml) {
            prop.put("mode_entries_" + number + "_page", entry.getPage());
            prop.put("mode_entries_" + number + "_timestamp", entry.getTimestamp());
        } else {
            prop.putWiki("mode_entries_" + number + "_page", entry.getPage());
        }

        if(hasRights) {
            prop.put("mode_entries_" + number + "_admin", "1");
            prop.put("mode_entries_" + number + "_admin_pageid",entry.getKey());
        } else {
            prop.put("mode_entries_" + number + "_admin", "0");
        }

        return prop;
    }
}
