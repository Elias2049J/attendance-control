const menuToggle = document.getElementById('menuToggle');
const sidebarMobile = document.getElementById('sidebarMobile');
const sidebarOverlay = document.getElementById('sidebarOverlay');
const navbar = document.querySelector('.navbar-landing');

let lastScrollTop = 0;
let scrollTimeout;

menuToggle.addEventListener('click', function () {
    sidebarMobile.classList.toggle('active');
    sidebarOverlay.classList.toggle('active');
});

sidebarOverlay.addEventListener('click', function () {
    sidebarMobile.classList.remove('active');
    sidebarOverlay.classList.remove('active');
});

if (window.innerWidth <= 768) {
    window.addEventListener('scroll', function () {
        clearTimeout(scrollTimeout);

        let scrollTop = window.pageYOffset || document.documentElement.scrollTop;

        if (scrollTop > lastScrollTop && scrollTop > 80) {
            navbar.classList.add('hidden');
        } else {
            navbar.classList.remove('hidden');
        }

        lastScrollTop = scrollTop <= 0 ? 0 : scrollTop;

        scrollTimeout = setTimeout(function () {
            navbar.classList.remove('hidden');
        }, 1000);
    });
}

document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function (e) {
        e.preventDefault();
        const target = document.querySelector(this.getAttribute('href'));

        if (target) {
            target.scrollIntoView({
                behavior: 'smooth',
                block: 'start'
            });

            sidebarMobile.classList.remove('active');
            sidebarOverlay.classList.remove('active');
        }
    });
});