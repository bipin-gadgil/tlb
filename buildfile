# COPY me to the PROJECT ROOT to USE me!

# require 'buildr/java/emma'

# Generated by Buildr 1.3.4, change to your liking
# Version number for this release
VERSION_NUMBER = "0.1-snapshot"
# Group identifier for your projects
GROUP = "cruise"

# Specify Maven 2.0 remote repositories here, like this:
repositories.remote << "http://www.ibiblio.org/maven2/"

desc "The Tlb project"

require 'misc/buildr_ext'

def tlb_layout
  layout = Layout.new
  layout[:source, :main, :java] = 'src'
  layout[:source, :test, :java] = 'test'
  layout[:target] = "target/tlb"
  layout[:root] = "."
  layout[:reports, :junit] = 'target/reports'
  layout[:reports, :emma] = 'target/emma'
  layout
end

LIB_JARS = Dir.glob('lib/*.jar')
TEST_JARS = Dir.glob('test/lib/*.jar')

define "tlb", :layout => tlb_layout  do |project|
  compile.options[:other] = %w[-encoding UTF-8]
  TMP_DIR = test.options[:properties]['java.io.tmpdir'] = _('target/temp')
  mkpath TMP_DIR

  manifest['Cruise-Version'] = VERSION_NUMBER

  project.version = VERSION_NUMBER
  project.group = GROUP

  resources.from(_('src')).exclude('*.java')
  compile.with LIB_JARS

  # emma.include("com.thoughtworks.*")

  test.resources.from(_('test')).exclude('*.java')

  test.with(TEST_JARS)

  clean do
    mkpath TMP_DIR
  end
end
