document.addEventListener('DOMContentLoaded', () => {
    // Toggle sidebar
    window.toggleSidebar = () => {
        document.querySelector('.sidebar').classList.toggle('open');
    };

    // Charts
    const resChart = document.getElementById('resChart');
    if (resChart) {
        new Chart(resChart, {
            type: 'line',
            data: {
                labels: ['Jan 1', 'Jan 5', 'Jan 10', 'Jan 15', 'Jan 20'],
                datasets: [{
                    label: 'Reservations',
                    data: [20, 35, 28, 45, 50],
                    borderColor: '#1B5E20',
                    backgroundColor: 'rgba(27,94,32,0.1)',
                    tension: 0.4,
                    fill: true
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } }
            }
        });
    }

    const occChart = document.getElementById('occChart');
    if (occChart) {
        new Chart(occChart, {
            type: 'doughnut',
            data: {
                labels: ['Occupied', 'Available'],
                datasets: [{
                    data: [75, 25],
                    backgroundColor: ['#28a745', '#6c757d']
                }]
            },
            options: { responsive: true, maintainAspectRatio: false }
        });
    }
});
