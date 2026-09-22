/* =============================================================================
   Library Management System — shared shell behaviour
   -----------------------------------------------------------------------------
   Built once, reused by every screen (design-system.md §5, CLAUDE.md rule 7).
   Plain JavaScript, no build step, no framework. Everything is driven by
   data-* attributes in the markup, so a feature template never needs its
   own script to use these components.

     1. Theme toggle          #theme-toggle
     2. Off-canvas sidebar    #sidebar-open, #sidebar, #sidebar-scrim
     3. Dropdowns             [data-dropdown] > [data-dropdown-trigger] + [data-dropdown-menu]
     4. Modal                 [data-modal-trigger="id"], [data-modal="id"], [data-modal-close]
     5. Toast                 LMS.toast.show({...}) or a button with data-toast-title

   Never alert() / confirm() — use the modal and toast (design-system.md §7).
   ============================================================================= */
(function () {
    'use strict';

    /* Motion durations are read from the CSS tokens so JS never disagrees
       with the stylesheet about how long an exit animation lasts. */
    function cssDurationMs(tokenName) {
        var raw = getComputedStyle(document.documentElement).getPropertyValue(tokenName).trim();
        var value = parseFloat(raw);
        if (isNaN(value)) return 0;
        return raw.endsWith('ms') ? value : value * 1000;
    }

    function prefersReducedMotion() {
        return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    }

    /* Runs `done` once the exit animation has finished — or at once if the
       user prefers reduced motion (the CSS collapses it to 1ms anyway). */
    function afterExitAnimation(done) {
        if (prefersReducedMotion()) { done(); return; }
        window.setTimeout(done, cssDurationMs('--duration-micro'));
    }


    /* -------------------------------------------------------------------------
       1. THEME TOGGLE
       The inline script in layout/base.html has already applied any saved
       choice before first paint. This only handles the click.
       ------------------------------------------------------------------------- */
    var THEME_KEY = 'lms-theme';

    function effectiveTheme() {
        var explicit = document.documentElement.getAttribute('data-theme');
        if (explicit === 'light' || explicit === 'dark') return explicit;
        return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    }

    function initThemeToggle() {
        var button = document.getElementById('theme-toggle');
        if (!button) return;

        function syncLabel() {
            var next = effectiveTheme() === 'dark' ? 'light' : 'dark';
            button.setAttribute('aria-label', 'Switch to ' + next + ' theme');
        }

        button.addEventListener('click', function () {
            var next = effectiveTheme() === 'dark' ? 'light' : 'dark';
            document.documentElement.setAttribute('data-theme', next);
            try {
                localStorage.setItem(THEME_KEY, next);
            } catch (e) { /* storage unavailable — choice lasts for this page only */ }
            syncLabel();
        });

        syncLabel();
    }


    /* -------------------------------------------------------------------------
       2. OFF-CANVAS SIDEBAR (< 768px)
       The icon rail (< 1024px) is pure CSS; only the drawer needs JS.
       ------------------------------------------------------------------------- */
    function initSidebar() {
        var sidebar = document.getElementById('sidebar');
        var openButton = document.getElementById('sidebar-open');
        var scrim = document.getElementById('sidebar-scrim');
        if (!sidebar || !openButton || !scrim) return;

        var offCanvasQuery = window.matchMedia('(max-width: 767.98px)');

        function open() {
            sidebar.classList.add('is-open');
            scrim.hidden = false;
            openButton.setAttribute('aria-expanded', 'true');
            document.body.classList.add('is-scroll-locked');
            var firstLink = sidebar.querySelector('a, button');
            if (firstLink) firstLink.focus();
        }

        function close(returnFocus) {
            if (!sidebar.classList.contains('is-open')) return;
            sidebar.classList.remove('is-open');
            scrim.hidden = true;
            openButton.setAttribute('aria-expanded', 'false');
            document.body.classList.remove('is-scroll-locked');
            if (returnFocus) openButton.focus();
        }

        openButton.addEventListener('click', open);
        scrim.addEventListener('click', function () { close(true); });

        document.addEventListener('keydown', function (event) {
            if (event.key === 'Escape') close(true);
        });

        // Following a link inside the drawer closes it.
        sidebar.addEventListener('click', function (event) {
            if (offCanvasQuery.matches && event.target.closest('a')) close(false);
        });

        // Widening the window past the breakpoint must not leave a stale
        // scroll lock or scrim behind.
        offCanvasQuery.addEventListener('change', function (event) {
            if (!event.matches) close(false);
        });
    }


    /* -------------------------------------------------------------------------
       3. DROPDOWNS
       ------------------------------------------------------------------------- */
    function initDropdowns() {
        var dropdowns = document.querySelectorAll('[data-dropdown]');

        function closeAll(except) {
            dropdowns.forEach(function (dropdown) {
                if (dropdown === except) return;
                var trigger = dropdown.querySelector('[data-dropdown-trigger]');
                var menu = dropdown.querySelector('[data-dropdown-menu]');
                if (menu && !menu.hidden) {
                    menu.hidden = true;
                    trigger.setAttribute('aria-expanded', 'false');
                }
            });
        }

        dropdowns.forEach(function (dropdown) {
            var trigger = dropdown.querySelector('[data-dropdown-trigger]');
            var menu = dropdown.querySelector('[data-dropdown-menu]');
            if (!trigger || !menu) return;

            trigger.addEventListener('click', function (event) {
                event.stopPropagation();
                var willOpen = menu.hidden;
                closeAll(dropdown);
                menu.hidden = !willOpen;
                trigger.setAttribute('aria-expanded', String(willOpen));
                if (willOpen) {
                    var firstItem = menu.querySelector('a, button');
                    if (firstItem) firstItem.focus();
                }
            });

            dropdown.addEventListener('keydown', function (event) {
                if (event.key === 'Escape' && !menu.hidden) {
                    event.stopPropagation();
                    menu.hidden = true;
                    trigger.setAttribute('aria-expanded', 'false');
                    trigger.focus();
                }
            });
        });

        document.addEventListener('click', function (event) {
            if (!event.target.closest('[data-dropdown]')) closeAll(null);
        });
    }


    /* -------------------------------------------------------------------------
       4. MODAL — raised, focus-trapped, Esc closes, returns focus on close
       Markup: <div class="modal-backdrop" data-modal="ID" hidden>
                 <div class="modal" role="dialog" aria-modal="true"> … </div>
               </div>
       Open with a [data-modal-trigger="ID"] button or LMS.modal.open('ID').
       ------------------------------------------------------------------------- */
    var FOCUSABLE = 'a[href], button:not([disabled]), input:not([disabled]), ' +
                    'select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

    var activeModal = null;
    var focusBeforeModal = null;

    function openModal(id) {
        var backdrop = document.querySelector('[data-modal="' + id + '"]');
        if (!backdrop || activeModal) return;

        focusBeforeModal = document.activeElement;
        activeModal = backdrop;
        backdrop.classList.remove('is-closing');
        backdrop.hidden = false;
        document.body.classList.add('is-scroll-locked');

        // Prefer the first control in the body over the header's close button.
        var body = backdrop.querySelector('.modal__body');
        var firstInBody = body ? body.querySelector(FOCUSABLE) : null;
        (firstInBody || backdrop.querySelector(FOCUSABLE) || backdrop).focus();
    }

    function closeModal() {
        if (!activeModal) return;
        var backdrop = activeModal;
        activeModal = null;

        backdrop.classList.add('is-closing');
        afterExitAnimation(function () {
            backdrop.hidden = true;
            backdrop.classList.remove('is-closing');
            document.body.classList.remove('is-scroll-locked');
            if (focusBeforeModal && typeof focusBeforeModal.focus === 'function') {
                focusBeforeModal.focus();
            }
            focusBeforeModal = null;
        });
    }

    function trapFocus(event) {
        if (!activeModal || event.key !== 'Tab') return;
        var focusables = Array.prototype.slice.call(activeModal.querySelectorAll(FOCUSABLE));
        if (focusables.length === 0) { event.preventDefault(); return; }

        var first = focusables[0];
        var last = focusables[focusables.length - 1];
        if (event.shiftKey && document.activeElement === first) {
            event.preventDefault();
            last.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
            event.preventDefault();
            first.focus();
        }
    }

    function initModals() {
        document.addEventListener('click', function (event) {
            var trigger = event.target.closest('[data-modal-trigger]');
            if (trigger) {
                openModal(trigger.getAttribute('data-modal-trigger'));
                return;
            }
            if (activeModal && event.target.closest('[data-modal-close]')) {
                closeModal();
                return;
            }
            // A click on the backdrop itself (not the dialog) closes.
            if (activeModal && event.target === activeModal) closeModal();
        });

        document.addEventListener('keydown', function (event) {
            if (!activeModal) return;
            if (event.key === 'Escape') {
                event.stopPropagation();
                closeModal();
            } else {
                trapFocus(event);
            }
        });
    }


    /* -------------------------------------------------------------------------
       5. TOAST — bottom-right, slides in, auto-dismiss 4s, manual close
       ------------------------------------------------------------------------- */
    var TOAST_AUTO_DISMISS_MS = 4000;   // design-system.md §5
    var TOAST_ICONS = { success: 'circle-check', error: 'triangle-alert', info: 'info' };

    function dismissToast(toast) {
        if (!toast || toast.classList.contains('is-leaving')) return;
        window.clearTimeout(toast._lmsTimer);
        toast.classList.add('is-leaving');
        afterExitAnimation(function () { toast.remove(); });
    }

    function svgIcon(name, className) {
        var ns = 'http://www.w3.org/2000/svg';
        var svg = document.createElementNS(ns, 'svg');
        svg.setAttribute('class', className);
        svg.setAttribute('aria-hidden', 'true');
        var use = document.createElementNS(ns, 'use');
        use.setAttribute('href', '/icons.svg#icon-' + name);
        svg.appendChild(use);
        return svg;
    }

    /**
     * Show a toast.
     * @param {{title: string, message?: string, variant?: 'success'|'error'|'info'}} options
     */
    function showToast(options) {
        var region = document.getElementById('toast-region');
        if (!region || !options || !options.title) return null;

        var variant = TOAST_ICONS[options.variant] ? options.variant : 'info';

        var toast = document.createElement('div');
        toast.className = 'toast toast--' + variant;
        // Errors interrupt assistive tech; everything else waits its turn.
        toast.setAttribute('role', variant === 'error' ? 'alert' : 'status');

        toast.appendChild(svgIcon(TOAST_ICONS[variant], 'icon toast__icon'));

        var content = document.createElement('div');
        content.className = 'toast__content';
        var title = document.createElement('p');
        title.className = 'toast__title';
        title.textContent = options.title;       // textContent, never innerHTML
        content.appendChild(title);
        if (options.message) {
            var message = document.createElement('p');
            message.className = 'toast__message';
            message.textContent = options.message;
            content.appendChild(message);
        }
        toast.appendChild(content);

        var close = document.createElement('button');
        close.type = 'button';
        close.className = 'icon-btn';
        close.setAttribute('aria-label', 'Dismiss notification');
        close.appendChild(svgIcon('x', 'icon icon--sm'));
        close.addEventListener('click', function () { dismissToast(toast); });
        toast.appendChild(close);

        region.appendChild(toast);
        toast._lmsTimer = window.setTimeout(function () { dismissToast(toast); }, TOAST_AUTO_DISMISS_MS);
        return toast;
    }

    function initToastTriggers() {
        document.addEventListener('click', function (event) {
            var trigger = event.target.closest('[data-toast-title]');
            if (!trigger) return;
            showToast({
                title: trigger.getAttribute('data-toast-title'),
                message: trigger.getAttribute('data-toast-message'),
                variant: trigger.getAttribute('data-toast-variant')
            });
        });
    }


    /* ------------------------------------------------------------------------- */
    window.LMS = {
        toast: { show: showToast, dismiss: dismissToast },
        modal: { open: openModal, close: closeModal }
    };

    function init() {
        initThemeToggle();
        initSidebar();
        initDropdowns();
        initModals();
        initToastTriggers();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
