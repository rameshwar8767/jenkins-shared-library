/**
 * hello.groovy
 * Utility greeting step for pipeline diagnostics and welcome banners.
 *
 * Usage:
 *   hello()
 *   hello('Alice')
 *   hello(name: 'DevOps Team', message: 'Starting production deployment')
 */
def call(String name = 'DevOps Engineer') {
    call(name: name)
}

def call(Map config = [:]) {
    def name = config.name ?: 'DevOps Engineer'
    def message = config.message ?: "Hello ${name}, welcome to Jenkins Shared Library CI/CD Pipeline!"
    echo message
}
