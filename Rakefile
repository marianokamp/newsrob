require 'open-uri'

SVG_DIR = "./svg/"
PNG_DIR = "./app/res/drawable/"
PNG_HDPI_DIR = "./app/res/drawable-hdpi-v4/"
PNG_LDPI_DIR = "./app/res/drawable-ldpi-v4/"
PNG_MDPI_DIR = "./app/res/drawable-mdpi-v4/"

BASE_DIMENSION = 40
BASE_DPI = 160

AMAZON_SRC_DIR = "./amazon/"
AMAZON_OUT_DIR = "./amazon_gen/"

BB_SRC_DIR = "./bb/"
BB_OUT_DIR = "./bb_gen/"

ANDROID_MARKET_SRC_DIR = "./android_market/"
ANDROID_MARKET_OUT_DIR = "./android_market_gen/"


CIRCLE_GREY = "#777"

TEMPLATE =<<END
<?xml version="1.0" standalone="no"?>
<!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" 
  "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
<svg width="12px" height="12px" 
     opacity="OPACITY"
     fill="none"
     stroke-width="3px"
     xmlns="http://www.w3.org/2000/svg" version="1.1">
  <defs>
     <circle id="content-circle" 
	      cx="12" cy="12" 
	      r="10"/>
	 <path id="half-circle" d="M2,16 C2,2 22,2 22,16" /> 
  </defs>
  <g transform="scale(0.5)">
    BODY
  </g>
</svg>
END

RED_DOT =<<END
    <circle cx="21" cy="21" r="3" fill="red"/>
END

USE_CONTENT_CIRCLE =<<END
    <use xlink:href='#content-circle' x='0' y='0' fill="#{CIRCLE_GREY}"/>
END

USE_HOLLOW_CONTENT_CIRCLE =<<END
    <use xlink:href='#content-circle' x='0' y='0' fill='none' stroke="#{CIRCLE_GREY}" stroke-width='3px'/>
END

USE_HALF_CIRCLE =<<END
    <use xlink:href="#half-circle" x="0" y="-4" fill="#{CIRCLE_GREY}" transform="rotate(90, 12, 12)"/>
END

BASIC_SHAPE_ARROW=<<END
  <g>
    <line x1="24" y1="11" x2="24" y2="36" style="stroke-linecap:round;stroke-width:#thick-stroke#;stroke:#stroke-color-black#"/>
    <polyline points="11,28 24,39 37,28 24,34 11,28" style="stroke-linecap:round;stroke-linejoin:round;stroke-width:#medium-stroke#;stroke:#stroke-color-black#;fill:#stroke-color-black#"/>
  </g>
END

BASIC_SHAPE_CIRCLE=<<END
  <g>
    <circle cx="23.5" cy="24" r="15.5"
            style="stroke-linecap:round;stroke-linejoin:round;stroke-width:#thick-stroke#;stroke:#stroke-color-black#;fill:#fill#" />
  </g>
END

BASIC_SHAPE_CIRCLE_WITH_EXTRA_INNER_CIRCLE=<<END
  <g>
    <circle cx="23.5" cy="24" r="15.5"
            style="stroke-linecap:round;stroke-linejoin:round;stroke-width:#thick-stroke#;stroke:#stroke-color-black#;fill:none" />
    <circle cx="23.5" cy="24" r="5"
            style="stroke-linecap:round;stroke-linejoin:round;stroke-width:#thick-stroke#;stroke:#stroke-color-black#;fill:#fill#" />
            
            
  </g>
END

SHAPE_CANCEL=<<END
  <g>
    <circle cx="23.5" cy="24" r="15.5"
            style="stroke-linecap:round;stroke-linejoin:round;stroke-width:#thick-stroke#;stroke:#stroke-color-black#" />
            <line x1="19" y1="19" x2="29" y2="29" style="stroke-linecap:round;stroke-linejoin:round;stroke-width:#medium-stroke#;stroke:#stroke-color-black#"/>
            <line x1="29" y1="19" x2="19" y2="29" style="stroke-linecap:round;stroke-linejoin:round;stroke-width:#medium-stroke#;stroke:#stroke-color-black#"/>
  </g>
END

SHAPE_SYNC=<<END
<g>
   <path d="M10,19 Q24,6 30,17" 
         style="stroke-linecap:round;stroke-linejoin:round;stroke-width:#thick-stroke#;stroke:#stroke-color-black#;" />
  <path d="M34,19 L34,9 L24,21 Z" 
         style="stroke-linecap:round;stroke-linejoin:round;stroke-width:#thin-stroke#;stroke:#stroke-color-black#;fill:#stroke-color-black#" />
</g>



<g transform="rotate(180, 24, 24) ">
  <g>
    <path d="M10,19 Q24,6 30,17" 
          style="stroke-linecap:round;stroke-linejoin:round;stroke-width:#thick-stroke#;stroke:#stroke-color-black#;" />
    <path d="M34,19 L34,9 L24,21 Z" 
          style="stroke-linecap:round;stroke-linejoin:round;stroke-width:#thin-stroke#;stroke:#stroke-color-black#;fill:#stroke-color-black#" />
  </g>
</g>

END

SHAPE_LIKE=<<END
<mask id="mask">
<g style="stroke:#000;fill:#000">
  <!--<rect x="0" y="0" width="100%" height="100%" style="fill:#f00"/>-->
  <circle cx="21.5" cy="21" r="20" 
     style="fill:#fff;stroke:#000"/>

  <circle cx="14" cy="14" r="2.0"
     style="stroke-linecap:round;stroke-linejoin:round;stroke-width:#medium-stroke#;"/>
  <circle cx="28" cy="14" r="2.0"
     style="stroke-linecap:round;stroke-linejoin:round;stroke-width:#medium-stroke#;"/>

  <path d="M8,24 Q21,42 34,24 "
    style="stroke-linecap:round;stroke-linejoin:round;stroke-width:#medium-stroke#;fill:none"/>

 <!-- <line x1="21" y1="12" x2="21" y2="24" 
    style="stroke-linecap:round;stroke-linejoin:round;stroke-width:#medium-stroke#;"/>-->

</g>
</mask>
<g><rect width="100%" height="100%" fill="url(#android_fill_gradient)" mask="url(#mask)" /></g>
END

SHAPE_CHECK_MARK=<<END
<g>
      <line x1="8" y1="24" x2="20" y2="38" 
            style="stroke-linecap:round;stroke-linejoin:round;stroke-width:#thick-stroke#;stroke:#stroke-color-black#;fill:#stroke-color-black#"/>
      <line x1="20" y1="38" x2="37" y2="8" 
            style="stroke-linecap:round;stroke-linejoin:round;stroke-width:#thick-stroke#;stroke:#stroke-color-black#;fill:#stroke-color-black#"/>
</g>
END

TRANSLATIONS = {"ultra-thick-stroke" => 8.0,
                "thick-stroke" => 6.0, 
                "medium-stroke" => 4.0,
                "thin-stroke" => 2.0,
                "stroke-color-black" => '#222',
                "fill" => '#222', 
                :opacity => "1.0"}
# 222
def rotate(src, degrees)
  "<g transform='rotate(#{degrees}, 24, 24)'>#{src}</g>\n"
end

def shrink_to_toolbar_icon(src)
  #src
  "<g transform='translate(6.5,5.3)'><g transform='scale(0.55)'>#{src}</g></g>\n"
  #"<g transform='translate(12,24)'><g transform='scale(0.5)'>#{src}</g></g>\n"
end

def add_svg_frame(src, width, height, scale, opacity)
<<END
  <?xml version="1.0" standalone="no"?>
  <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" 
    "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
    <!--    <svg width="#{width+2}px" height="#{height+2}px" -->
    <svg width="#{width}px" height="#{height}px" 
       fill="none"
       xmlns="http://www.w3.org/2000/svg"
       xmlns:xlink="http://www.w3.org/1999/xlink"
       version="1.1">
       
       <defs>
         <linearGradient id="android_fill_gradient" angle="270" x1="0%" y1="0%" x2="0%" y2="100%">
           <stop offset="0%" style="stop-color:rgb(163,163,163)"/>
           <stop offset="100%" style="stop-color:rgb(120,120,120)"/>
         </linearGradient>
       </defs>
       
       <!-- 9patch -->
       <!--<g shape-rendering="crispEdges"> 
         <line x1="1" x2="1" y1="0" y2="0" style="stroke:#000;stroke-width:1px"/>
         <line x1="#{width-1}" x2="#{width-1}" y1="0" y2="0" style="stroke:#000;stroke-width:1px"/>
         <line x1="0" x2="0" y1="2" y2="#{26*scale}" style="stroke:#000;stroke-width:1px"/>
        
       </g>-->
        <!--<line x1="0" x2="0" y1="#{height-1}" y2="#{height-1}" style="stroke:#000;stroke-width:1px"/>-->
       
       <clipPath id="clip">
             <rect x="0" y="0" width="#{width}" height="#{height}" />
       </clipPath>   
    <g clip-path="url(#clip)" transform="translate(1,1)">
      <g transform="scale(#{scale})" opacity="#{opacity}">
        #{src}
      </g>
    </g>
  </svg>
END
end

def tidy(src)
  IO.popen "tidy -i -xml 2>/dev/null", "w+" do |p|
    p.write(src)
    p.close_write
    p.read
  end
end

def translate(src, translations)
  translations.each do |k,v|
    src = src.gsub("##{k}#", v.to_s)
  end
  src
end

def add_focused_glow(src, options, translations)
  puts "Add Focused Glow called."
  src_orig = String.new src
  src_glow = translate(String.new(src), translations.merge({"stroke-color-black" => '#ffa500', "fill" => '#ffa500'}))
 
<<END
  <g transform="translate(-11, -21.5), scale(1.5)">
    #{src_glow}
  </g>
  #{src}
END
end

def add_grid(src)
  src << <<END  
  <rect 
        x="3" 
        y="3" 
        width="40" 
        height="40" 
        style="stroke:#f00;stroke-width:1px;fill:none" />
     <rect 
        x="6" 
        y="6" 
        width="35" 
        height="35" 
        style="stroke:#ff0;stroke-width:1px;fill:none" />
END
end

task :generate_toolbar_icons do

  show_grid = true
  
  generate_toolbar_icon "toolbar_icon_sort_order_descending", BASIC_SHAPE_ARROW, []
  generate_toolbar_icon "toolbar_icon_sort_order_ascending",  rotate(BASIC_SHAPE_ARROW, 180), []

  generate_toolbar_icon "toolbar_icon_sync", SHAPE_SYNC, []
  
  
  generate_toolbar_icon "toolbar_icon_sync", SHAPE_SYNC, [:disabled]
#  generate_toolbar_icon "toolbar_icon_cancel", SHAPE_CANCEL, []
  
  generate_toolbar_icon("toolbar_icon_show", BASIC_SHAPE_CIRCLE, [])
  
  generate_toolbar_icon("toolbar_icon_show_with_pinned", BASIC_SHAPE_CIRCLE_WITH_EXTRA_INNER_CIRCLE, [])

  generate_toolbar_icon("toolbar_icon_hide", translate(BASIC_SHAPE_CIRCLE, {"fill" => 'none'}), [])

  generate_toolbar_icon("toolbar_icon_mark_all_read", SHAPE_CHECK_MARK, [:disabled] )
  
  generate_toolbar_icon "toolbar_icon_like", SHAPE_LIKE, []
  
  
end

def generate_bitmap_from_svg(options)
  File.open("/tmp/source_#{options[:name]}_#{options[:dpi]}.svg", 'w') {|f| f.write(options[:source])}
  File.open("/tmp/source.svg", 'w') {|f| f.write(options[:source])}
  output_file_name= "#{options[:output_dir]}gen_#{options[:name]}.png"
#  output_file_name= "#{options[:output_dir]}gen_#{options[:name]}.9.png"
  command = "java -Djava.awt.headless=true -jar batik-1.7/batik-rasterizer.jar /tmp/source.svg -d #{output_file_name} -dpi #{options[:dpi]}"
  system command  
end

def generate_pressed_drawable(name)
  src=<<END
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">	    
  <item>
    <shape>
   	  <corners android:radius="5dp"/>
   		<gradient android:type="radial" android:startColor="#E77A26" android:endColor="#ea2" android:gradientRadius="70"/>
    </shape>
  </item>
  <item android:drawable="@drawable/gen_#{name}" />
</layer-list>
END

File.open("#{PNG_DIR}gen_#{name}_pressed.xml", 'w') {|f|f.write(src)}
 
end

def add_grey_background_gradient(src)
  src=<<END
    <mask id="mask">#{src}</mask>
    <g><rect width="100%" height="100%" fill="url(#android_fill_gradient)" mask="url(#mask)" /></g>
END
end

def generate_selector(name, states)
  if states.include? :disabled
    disabled=<<END
    <item android:state_enabled="false"
  		  android:drawable="@drawable/gen_#{name}_disabled" />
END
  else
    disabled=""
  end
  
  src=<<END
<?xml version="1.0" encoding="utf-8"?>
  <selector xmlns:android="http://schemas.android.com/apk/res/android">
	    <item android:state_focused="true" android:state_enabled="true"
		      android:drawable="@drawable/gen_#{name}_focused" />
        	<item android:state_pressed="true"
        		  android:drawable="@drawable/gen_#{name}_pressed" />
          <item android:state_selected="true"
          	  android:drawable="@drawable/gen_#{name}_pressed" />
  	#{disabled}
  	<item android:drawable="@drawable/gen_#{name}" />
  </selector>
END
  File.open("#{PNG_DIR}gen_#{name}_selector.xml", 'w') {|f|f.write(src)}
end

def create_options(name, scale, output_dir)
  options = {}
  
  options[:name] = name
  options[:scale]  = scale
  options[:width]  = (BASE_DIMENSION * scale).round
  options[:height] = (BASE_DIMENSION * scale).round
  options[:dpi]    = (BASE_DPI * scale).round
 
  options[:output_dir] = output_dir  
  options.clone
end

def generate_toolbar_icon(name, basic_shape, extra_states, translations = TRANSLATIONS)
  puts "Generate toolbar icon for #{name}."
  all_states = [:enabled] # , :focused, :pressed
  all_states << extra_states
  all_states.flatten!
   
  src = String.new basic_shape
  src = shrink_to_toolbar_icon(src)

  if false then
    src = add_grid(src)
  end
  
  all_options = [create_options(name, 1.0, PNG_DIR)]
  all_options << create_options(name, 1.0, PNG_MDPI_DIR)
  all_options << create_options(name, 1.5, PNG_HDPI_DIR)
  all_options << create_options(name, 0.75, PNG_LDPI_DIR) # 66666667
  
  all_states.each do |state|
    
    local_translations = translations.clone
    
    all_options.each do |options|
      
      src2 = String.new src
      
      options = options.clone
     
         local_translations["stroke-color-black"] = '#fff'
         local_translations["fill"] = '#fff'
     
     if false
      case state
      when :enabled
        #src2 = add_grid_background_gradient(src2)
        local_translations["stroke-color-black"] = '#666'
        local_translations["fill"] = '#666'
      when :disabled
        local_translations["stroke-color-black"] = '#666'
        local_translations["fill"] = '#666'
        local_translations[:opacity] = "0.3"
        options[:name] = options[:name]+"_disabled"
      when :focused
        local_translations["stroke-color-black"] = '#444'
        local_translations["fill"] = '#444'
        options[:name] = options[:name]+"_focused"
        src3 = String.new src2
        lt = local_translations.clone
        #factor = 2.2 #if options[:scale] == 0.75 then 2.2 else 1.5 end 
        # puts factor
        
        lt.each do |k,v|
          #lt[k] = v.to_f * factor  if k =~ /-stroke/
          lt[k] = 17.0 if k =~ /-stroke/
          
        end
        lt.merge!({"stroke-color-black" => '#ffa500', "fill" => '#ffa500'})
        require 'pp'
        src3 = translate(src3, lt)
        src2 = src3 + src2
      when :pressed
        next
      end
    end
       if false && options[:output_dir] == "#{PNG_LDPI_DIR}" then
        local_translations["stroke-color-black"] = '#f33'
        local_translations["fill"] = '#f33'
      end

      src2 = add_svg_frame(src2, options[:width], options[:height], options[:scale], local_translations[:opacity])
      src2 = translate(src2, local_translations)
      src2 = tidy(src2)
      options[:source] = src2
  
      generate_bitmap_from_svg(options)

    end
    #generate_selector(name, all_states)
    #generate_pressed_drawable(name)
  end
end

file :generate_bitmaps => Dir["#{SVG_DIR}*.svg"] do |t|
  t.prerequisites.each do |svg_file|
    puts "!svg_file!=#{svg_file}"
    next if svg_file =~ /_defs.svg$/
    
    svg_file =~/.*\/(.*?)\.svg$/
    name = $1
    next if name =~ /^menu_/
    next if name =~ /toolbar_/

    src = "#{SVG_DIR}#{name}.svg"
    
    dpi = 0
    if svg_file =~/.*_hdpi/
      dpi = 240
      name = name.gsub(/_hdpi/, "")
      dst = "#{PNG_HDPI_DIR}gen_#{name}.png"
    else
      dpi = 160
      dst = "#{PNG_DIR}gen_#{name}.png"
    end
    
    unless uptodate?(dst, src) 
      command = "java -Djava.awt.headless=true -jar batik-1.7/batik-rasterizer.jar #{src} -d #{dst} -dpi #{dpi}"
      system command
    end
  end
end

# obsolete
task :generate_menu_svgs do 

  Dir["#{SVG_DIR}menu*.svg"].each do |t|
    t =~/.*\/(.*?)\.svg$/
    svg_file = $1
    ldpi_src = "#{SVG_DIR}m_#{$1}.svg"
    hdpi_src = "#{SVG_DIR}m_#{$1}_hdpi.svg"

    src = IO.read(t)+"\n"
    File.open(ldpi_src, 'w') {|f| f.write(src)}
    src.gsub!(/24px/, '36px')
    src.gsub!(/scale\(1.0\)/, 'scale(1.50)')
    File.open(hdpi_src, 'w') {|f| f.write(src)}
    
  end

end

task :generate_svgs do |t|
	
	all_markers = []

	all_markers << ['downloaded_no_web',    0.3, USE_CONTENT_CIRCLE]
	all_markers << ['downloaded_yes_web',   1.0, USE_CONTENT_CIRCLE]
	all_markers << ['downloaded_error_web', 1.0, USE_CONTENT_CIRCLE+RED_DOT]

	all_markers << ['downloaded_no_feed',    0.3, USE_HOLLOW_CONTENT_CIRCLE+USE_HALF_CIRCLE]
	all_markers << ['downloaded_yes_feed',   1.0, USE_HOLLOW_CONTENT_CIRCLE+USE_HALF_CIRCLE]
	all_markers << ['downloaded_error_feed', 1.0, USE_HOLLOW_CONTENT_CIRCLE+USE_HALF_CIRCLE+RED_DOT]
	
	all_markers << ['downloaded_no_headers',    0.3, USE_HOLLOW_CONTENT_CIRCLE]
	all_markers << ['downloaded_yes_headers',   1.0, USE_HOLLOW_CONTENT_CIRCLE]
	all_markers << ['downloaded_error_headers', 1.0, USE_HOLLOW_CONTENT_CIRCLE+RED_DOT]
	
	all_markers.each do |desc|
	  file_name = "#{SVG_DIR}m_#{desc[0]}.svg"
  	puts "writing #{file_name} ..."
  	
		body = TEMPLATE
		body = body.gsub(/OPACITY/, desc[1].to_s)
		body = body.gsub(/BODY/,    desc[2])
  	  
  	f = File.new(file_name, 'w') 
  	f.puts body
  	f.close
  
    file_name = "#{SVG_DIR}m_#{desc[0]}_hdpi.svg"
    puts "writing #{file_name} ..."
    body = body.gsub(/scale\(0.5\)/, "scale(0.75)")
    body = body.gsub(/12px/, "18px")

  	f = File.new(file_name, 'w') 
  	f.puts body
  	f.close

	end 	
end

task :generate_notification_icon_svg do
   body = IO.read "#{SVG_DIR}logo.svg.template"
  # body.gsub! /<!-- REPLACE ME1 -->/, <<END
  #  <g>
  #    <rect x="2" y="2" width="20" height="20" rx="2" ry="2" fill="url(#grey_gradient)"/>
  #  </g>
  # END
   body.gsub! /transform=\'scale\(0\.05,0\.45\)\'/, "transform='scale(0.038, 0.035) translate(63,63)'"
   body.gsub! /width="500px" height="500px"/, "width='25px' height='25px'"
  
   f = File.new("#{SVG_DIR}auto_action_bar_newsrob_icon.svg" , 'w') 
   f.puts body
   f.close
  
   body.gsub! /url\(#orange_gradient\)/, "url(#grey_gradient)" #999
   body.gsub! /url\(#blue_gradient\)/, "url(#fill_gradient)"  #666

   f = File.new("#{SVG_DIR}auto_notification_icon.svg" , 'w') 
   f.puts body
 	 f.close

  
   body = IO.read "#{SVG_DIR}logo.svg.template"
  # body.gsub! /<!-- REPLACE ME1 -->/, <<END
  #  <g>
  #    <rect x="2" y="2" width="20" height="20" rx="2" ry="2" fill="url(#grey_gradient)"/>
  #  </g>
  # END
   body.gsub! /transform=\'scale\(0\.05,0\.45\)\'/, "transform='scale(0.057, 0.0525) translate(63,63)'"
   body.gsub! /width="500px" height="500px"/, "width='38px' height='38px'"
   body.gsub! /url\(#orange_gradient\)/, "url(#grey_gradient)" #999
   body.gsub! /url\(#blue_gradient\)/, "url(#fill_gradient)"  #666

   f = File.new("#{SVG_DIR}auto_notification_icon_hdpi.svg" , 'w') 
   f.puts body
 	 f.close

    body = IO.read "#{SVG_DIR}logo.svg.template"
   # body.gsub! /<!-- REPLACE ME1 -->/, <<END
   #  <g>
   #    <rect x="2" y="2" width="20" height="20" rx="2" ry="2" fill="url(#grey_gradient)"/>
   #  </g>
   # END
    body.gsub! /transform=\'scale\(0\.05,0\.45\)\'/, "transform='scale(0.038, 0.035) translate(63,63)'"
    body.gsub! /width="500px" height="500px"/, "width='25px' height='25px'"
    body.gsub! /url\(#orange_gradient\)/, "url(#grey_gradient)" #999
    body.gsub! /url\(#blue_gradient\)/, "url(#fill_gradient)"  #666
    body.gsub! /white/, "red" 

    f = File.new("#{SVG_DIR}auto_notification_sync_problem.svg" , 'w') 
    f.puts body
  	f.close

    body = IO.read "#{SVG_DIR}logo.svg.template"
   # body.gsub! /<!-- REPLACE ME1 -->/, <<END
   #  <g>
   #    <rect x="2" y="2" width="20" height="20" rx="2" ry="2" fill="url(#grey_gradient)"/>
   #  </g>
   # END
    body.gsub! /transform=\'scale\(0\.05,0\.45\)\'/, "transform='scale(0.057, 0.0525) translate(63,63)'"
    body.gsub! /width="500px" height="500px"/, "width='38px' height='38px'"
    body.gsub! /url\(#orange_gradient\)/, "url(#grey_gradient)" #999
    body.gsub! /url\(#blue_gradient\)/, "url(#fill_gradient)"  #666
    body.gsub! /white/, "red" 

    f = File.new("#{SVG_DIR}auto_notification_sync_problem_hdpi.svg" , 'w') 
    f.puts body
  	f.close
  	
  	
  	body = IO.read "#{SVG_DIR}logo.svg.template"
    # body.gsub! /<!-- REPLACE ME1 -->/, <<END
    #  <g>
    #    <rect x="2" y="2" width="20" height="20" rx="2" ry="2" fill="url(#grey_gradient)"/>
    #  </g>
    # END
     body.gsub! /transform=\'scale\(0\.05,0\.45\)\'/, "transform='scale(0.072960005, 0.067200004) translate(63,63)'"
     body.gsub! /width="500px" height="500px"/, "width='48px' height='48px'"
     body.gsub! /url\(#orange_gradient\)/, "url(#grey_gradient)" #999
     body.gsub! /url\(#blue_gradient\)/, "url(#fill_gradient)"  #666

     f = File.new("#{SVG_DIR}auto_app_icon.svg" , 'w') 
     f.puts body
   	 f.close
    
     body = IO.read "#{SVG_DIR}logo.svg.template"
     # body.gsub! /<!-- REPLACE ME1 -->/, <<END
     #  <g>
     #    <rect x="2" y="2" width="20" height="20" rx="2" ry="2" fill="url(#grey_gradient)"/>
     #  </g>
     # END
      body.gsub! /transform=\'scale\(0\.05,0\.45\)\'/, "transform='scale(0.10799995, 0.099473642) translate(63,63)'"
      body.gsub! /width="500px" height="500px"/, "width='72px' height='72px'"
      body.gsub! /url\(#orange_gradient\)/, "url(#grey_gradient)" #999
      body.gsub! /url\(#blue_gradient\)/, "url(#fill_gradient)"  #666

      f = File.new("#{SVG_DIR}auto_app_icon_hdpi.svg" , 'w') 
      f.puts body
    	 f.close
	
  
end


file :generate_android_market_assets => Dir["#{ANDROID_MARKET_SRC_DIR}*.svg"] do |t|
  t.prerequisites.each do |svg_file|
    puts "svg_file=#{svg_file}"
    
    svg_file =~/.*\/(.*?)\.svg$/
    name = $1
  
    src = "#{ANDROID_MARKET_SRC_DIR}#{name}.svg"
    dst = "#{ANDROID_MARKET_OUT_DIR}#{name}.png".gsub!(/VERSION/, '4.4.0')
    
    unless uptodate?(dst, src) 
      command = "java -Djava.awt.headless=true -jar batik-1.7/batik-rasterizer.jar #{src} -d #{dst}"
      system command
    end
  end
end

file :generate_bb_assets => Dir["#{BB_SRC_DIR}*.svg"] do |t|
  t.prerequisites.each do |svg_file|
    puts "svg_file=#{svg_file}"
    
    svg_file =~/.*\/(.*?)\.svg$/
    name = $1
  
    src = "#{BB_SRC_DIR}#{name}.svg"
    dst = "#{BB_OUT_DIR}#{name}.png"
    
    unless uptodate?(dst, src) 
      command = "java -Djava.awt.headless=true -jar batik-1.7/batik-rasterizer.jar #{src} -d #{dst}"
      system command
    end
  end
end

file :generate_amazon_assets => Dir["#{AMAZON_SRC_DIR}*.svg"] do |t|
  t.prerequisites.each do |svg_file|
    puts "svg_file=#{svg_file}"
    
    svg_file =~/.*\/(.*?)\.svg$/
    name = $1
  
    src = "#{AMAZON_SRC_DIR}#{name}.svg"
    dst = "#{AMAZON_OUT_DIR}#{name}.png".gsub!(/VERSION/, '4.3.0')
    
    unless uptodate?(dst, src) 
      command = "java -Djava.awt.headless=true -jar batik-1.7/batik-rasterizer.jar #{src} -d #{dst}"
      system command
    end
  end
end


task :clean do
  Dir["#{PNG_DIR}gen_*.png"].each {|f| rm f rescue nil}
  Dir["#{PNG_DIR}gen_*.xml"].each {|f| rm f rescue nil}
  Dir["#{PNG_HDPI_DIR}gen_*.png"].each {|f| rm f rescue nil}
  Dir["#{PNG_LDPI_DIR}gen_*.png"].each {|f| rm f rescue nil}
  Dir["#{PNG_MDPI_DIR}gen_*.png"].each {|f| rm f rescue nil}
  Dir["#{AMAZON_OUT_DIR}gen_*.png"].each {|f| rm f rescue nil}
  
end
task :default => [:generate_svgs,  :generate_toolbar_icons, :generate_notification_icon_svg, :generate_bitmaps]

