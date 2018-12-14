/*
 * Copyright 1998-2018 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package ru.org.linux.util.markdown

import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.youtube.embedded.YouTubeLinkExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.MutableDataSet
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

import scala.collection.JavaConverters._

@Service
@Qualifier("flexmark")
class FlexmarkMarkdownFormatter() extends MarkdownFormatter {
  private val options = {
    val options = new MutableDataSet

    options.set(Parser.EXTENSIONS, Seq(TablesExtension.create, StrikethroughExtension.create,
      YouTubeLinkExtension.create, new SuppressImagesExtension).asJava)

    options.set(HtmlRenderer.SUPPRESSED_LINKS, "javascript:.*")
    options.set(HtmlRenderer.SUPPRESS_HTML, Boolean.box(true))

    // uncomment to convert soft-breaks to hard breaks
    //options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");

    options.toImmutable
  }

  private val parser = Parser.builder(options).build
  private val renderer = HtmlRenderer.builder(options).build

  override def renderToHtml(content: String): String = {
    // You can re-use parser and renderer instances
    val document = parser.parse(content)
    renderer.render(document)
  }
}