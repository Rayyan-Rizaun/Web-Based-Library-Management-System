(function () {
    'use strict';

    function cssVar(name) {
        return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
    }

    function fontFamily() {
        var raw = cssVar('--font-ui');
        return raw || "'Public Sans', system-ui, sans-serif";
    }

    var NO_HOVER_ANIMATION = { active: { animation: { duration: 0 } } };

    function baseOptions(titleText, legend) {
        var ink2 = cssVar('--ink-2');
        var border = cssVar('--border');
        return {
            responsive: true,
            maintainAspectRatio: false,
            animation: { duration: 400 },
            transitions: NO_HOVER_ANIMATION,
            plugins: {
                title: {
                    display: true,
                    text: titleText,
                    color: cssVar('--ink'),
                    font: { family: fontFamily(), size: 14, weight: '600' }
                },
                legend: {
                    display: legend,
                    labels: { color: ink2, font: { family: fontFamily(), size: 12 } }
                }
            },
            scales: {
                x: {
                    ticks: { color: ink2, font: { family: fontFamily(), size: 11 } },
                    grid: { color: border },
                    border: { color: border }
                },
                y: {
                    ticks: { color: ink2, font: { family: fontFamily(), size: 11 } },
                    grid: { color: border },
                    border: { color: border },
                    beginAtZero: true
                }
            }
        };
    }

    function axisTitles(options, xTitle, yTitle) {
        if (xTitle) {
            options.scales.x.title = { display: true, text: xTitle, color: cssVar('--ink-2'), font: { family: fontFamily(), size: 12 } };
        }
        if (yTitle) {
            options.scales.y.title = { display: true, text: yTitle, color: cssVar('--ink-2'), font: { family: fontFamily(), size: 12 } };
        }
        return options;
    }

    var registry = [];

    function register(canvasId, factory) {
        var entry = { canvasId: canvasId, factory: factory, chart: factory() };
        registry.push(entry);
        return entry.chart;
    }

    function redrawAll() {
        registry.forEach(function (entry) {
            if (entry.chart) entry.chart.destroy();
            entry.chart = entry.factory();
        });
    }

    var observer = new MutationObserver(function (mutations) {
        for (var i = 0; i < mutations.length; i++) {
            if (mutations[i].attributeName === 'data-theme') {
                redrawAll();
                return;
            }
        }
    });
    observer.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] });

    function line(canvasId, data, opts) {
        opts = opts || {};
        return register(canvasId, function () {
            var canvas = document.getElementById(canvasId);
            if (!canvas) return null;
            var color = opts.color || cssVar('--primary');
            var options = axisTitles(baseOptions(opts.title, data.datasets.length > 1), opts.xTitle, opts.yTitle);
            return new Chart(canvas, {
                type: 'line',
                data: {
                    labels: data.labels,
                    datasets: data.datasets.map(function (ds, i) {
                        var c = (opts.colors && opts.colors[i]) || color;
                        return {
                            label: ds.label,
                            data: ds.values,
                            borderColor: c,
                            backgroundColor: c,
                            tension: 0.25,
                            pointRadius: 3,
                            fill: false
                        };
                    })
                },
                options: options
            });
        });
    }

    function bar(canvasId, data, opts) {
        opts = opts || {};
        return register(canvasId, function () {
            var canvas = document.getElementById(canvasId);
            if (!canvas) return null;
            var defaultColors = [cssVar('--primary'), cssVar('--accent')];
            var options = axisTitles(baseOptions(opts.title, data.datasets.length > 1), opts.xTitle, opts.yTitle);
            return new Chart(canvas, {
                type: 'bar',
                data: {
                    labels: data.labels,
                    datasets: data.datasets.map(function (ds, i) {
                        return {
                            label: ds.label,
                            data: ds.values,
                            backgroundColor: (opts.colors && opts.colors[i]) || defaultColors[i % defaultColors.length]
                        };
                    })
                },
                options: options
            });
        });
    }

    function horizontalBar(canvasId, data, opts) {
        opts = opts || {};
        return register(canvasId, function () {
            var canvas = document.getElementById(canvasId);
            if (!canvas) return null;
            var color = opts.color || cssVar('--accent');
            var options = axisTitles(baseOptions(opts.title, data.datasets.length > 1), opts.xTitle, opts.yTitle);
            options.indexAxis = 'y';
            return new Chart(canvas, {
                type: 'bar',
                data: {
                    labels: data.labels,
                    datasets: data.datasets.map(function (ds) {
                        return { label: ds.label, data: ds.values, backgroundColor: color };
                    })
                },
                options: options
            });
        });
    }

    function doughnut(canvasId, data, opts) {
        opts = opts || {};
        return register(canvasId, function () {
            var canvas = document.getElementById(canvasId);
            if (!canvas) return null;
            var colors = opts.colors || [cssVar('--primary'), cssVar('--accent'), cssVar('--overdue'), cssVar('--due-soon'), cssVar('--reserved'), cssVar('--on-loan'), cssVar('--available')];
            var options = {
                responsive: true,
                maintainAspectRatio: false,
                animation: { duration: 400 },
                transitions: NO_HOVER_ANIMATION,
                plugins: {
                    title: {
                        display: true,
                        text: opts.title,
                        color: cssVar('--ink'),
                        font: { family: fontFamily(), size: 14, weight: '600' }
                    },
                    legend: {
                        display: true,
                        position: 'right',
                        labels: { color: cssVar('--ink-2'), font: { family: fontFamily(), size: 12 } }
                    }
                }
            };
            return new Chart(canvas, {
                type: 'doughnut',
                data: {
                    labels: data.labels,
                    datasets: [{
                        data: data.datasets[0].values,
                        backgroundColor: data.labels.map(function (_, i) { return colors[i % colors.length]; }),
                        borderColor: cssVar('--surface'),
                        borderWidth: 2
                    }]
                },
                options: options
            });
        });
    }

    window.LMS = window.LMS || {};
    window.LMS.charts = { line: line, bar: bar, horizontalBar: horizontalBar, doughnut: doughnut, cssVar: cssVar };
})();
